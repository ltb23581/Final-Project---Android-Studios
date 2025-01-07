package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoommateExpensesFragment extends Fragment {
    private RecyclerView recyclerView;
    private RoommateExpensesAdapter adapter;
    private Map<String, Double> roommateSpendingMap = new HashMap<>();
    private DatabaseReference expensesReference;
    private TextView totalSpentTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_roommate_expenses, container, false);
        initializeViews(rootView);
        fetchExpenses();
        return rootView;
    }

    private void initializeViews(View rootView) {
        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        totalSpentTextView = rootView.findViewById(R.id.total_spent);
        expensesReference = FirebaseDatabase.getInstance().getReference("expenses");
        adapter = new RoommateExpensesAdapter(roommateSpendingMap);
        recyclerView.setAdapter(adapter);
    }

    private void fetchExpenses() {
        expensesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roommateSpendingMap.clear();
                double totalSpent = 0;

                for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot dateSnapshot : groupSnapshot.getChildren()) {
                        for (DataSnapshot userSnapshot : dateSnapshot.getChildren()) {
                            String userEmail = userSnapshot.getKey().replace(",", ".");
                            double userTotal = calculateUserTotal(userSnapshot);

                            roommateSpendingMap.merge(userEmail, userTotal, Double::sum);
                            totalSpent += userTotal;
                        }
                    }
                }

                updateUI(totalSpent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load expenses: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double calculateUserTotal(DataSnapshot userSnapshot) {
        double total = 0;
        DataSnapshot itemsSnapshot = userSnapshot.child("items");
        for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
            HelperClass.ShoppingItem item = itemSnapshot.getValue(HelperClass.ShoppingItem.class);
            if (item != null && item.getPrice() != null) {
                total += item.getPrice();
            }
        }
        return total * 1.08; // Including 8% tax
    }

    private void updateUI(double totalSpent) {
        double averageSpent = totalSpent / roommateSpendingMap.size();
        totalSpentTextView.setText(String.format("Total Spending: $%.2f", totalSpent));
        adapter.updateData(roommateSpendingMap, totalSpent, averageSpent);
    }
}