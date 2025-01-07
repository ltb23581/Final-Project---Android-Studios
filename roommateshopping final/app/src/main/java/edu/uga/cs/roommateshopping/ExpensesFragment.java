package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpensesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ExpensesAdapter adapter;
    private List<HelperClass.ShoppingItem> purchases;
    private DatabaseReference expensesReference;

    private String currentDate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.group_expenses, container, false);

        recyclerView = rootView.findViewById(R.id.expenses_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        purchases = new ArrayList<>();
        adapter = new ExpensesAdapter(purchases);
        recyclerView.setAdapter(adapter);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        currentDate = dateFormat.format(new java.util.Date());

        // Firebase reference to expenses
        expensesReference = FirebaseDatabase.getInstance().getReference("expenses");

        fetchExpenses();

        return rootView;
    }

    private void fetchExpenses() {
        expensesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                purchases.clear();

                for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                    String groupId = groupSnapshot.getKey();
                    String date = "";
                    double totalSubtotal = 0;
                    double totalTaxes = 0;
                    double totalAmount = 0;

                    String email = "";
                    for (DataSnapshot dateTimeSnapshot : groupSnapshot.getChildren()) {
                        date = dateTimeSnapshot.getKey();
                        for (DataSnapshot emailSnapshot : dateTimeSnapshot.getChildren()){
                            email = emailSnapshot.getKey();
                            String userEmail = email.replace(",", ".");

                            HelperClass.ShoppingItem groupHeader = new HelperClass.ShoppingItem();
                            groupHeader.setGroupId(groupId);
                            groupHeader.setItemName("Roommate: " + userEmail);
                            groupHeader.setDate(date);
                            purchases.add(groupHeader);
                            DataSnapshot itemsSnapshot = emailSnapshot.child("items");
                            for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                HelperClass.ShoppingItem item = itemSnapshot.getValue(HelperClass.ShoppingItem.class);
                                if (item != null) {
                                    item.setItemAdded(true);
                                    item.setGroupId(groupId);
                                    item.setDate(date);
                                    DatabaseReference itemRef = itemSnapshot.getRef();
                                    itemRef.setValue(item);
                                    purchases.add(item);
                                    totalSubtotal += item.getPrice();
                                }
                            }
                        }
                    }

                    double taxes = totalSubtotal * 0.08;
                    double total = totalSubtotal + taxes;

                    HelperClass.ShoppingItem priceHeader = new HelperClass.ShoppingItem();
                    priceHeader.setItemName(String.format("Subtotal: $%.2f\nTaxes: $%.2f\nTotal: $%.2f", totalSubtotal, taxes, total));
                    purchases.add(priceHeader);

                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load expenses: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
