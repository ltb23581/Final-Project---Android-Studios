package edu.uga.cs.roommateshopping;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ExpensesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<HelperClass.ShoppingItem> purchases;
    private final DatabaseReference expensesReference;

    private static final int TYPE_GROUP_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_TOTALS = 2;

    public ExpensesAdapter(List<HelperClass.ShoppingItem> purchases) {
        this.purchases = purchases;
        this.expensesReference = FirebaseDatabase.getInstance().getReference("expenses");
    }

    @Override
    public int getItemViewType(int position) {
        HelperClass.ShoppingItem item = purchases.get(position);
        String itemName = item.getItemName();

        if (!TextUtils.isEmpty(itemName)) {
            if (itemName.startsWith("Roommate: ")) {
                return TYPE_GROUP_HEADER;
            } else if (itemName.startsWith("Subtotal:") || itemName.startsWith("Taxes:") || itemName.startsWith("Total:")) {
                return TYPE_TOTALS;
            }
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_GROUP_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_group_header, parent, false);
            return new GroupHeaderViewHolder(view);
        } else if (viewType == TYPE_TOTALS) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.group_totals, parent, false);
            return new TotalsViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_expenses, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HelperClass.ShoppingItem item = purchases.get(position);

        if (holder instanceof GroupHeaderViewHolder) {
            GroupHeaderViewHolder groupHolder = (GroupHeaderViewHolder) holder;
            groupHolder.groupName.setText(item.getItemName());
            groupHolder.dateLabel.setText("Date: " + item.getDate());
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            itemHolder.itemName.setText(item.getItemName());
            itemHolder.itemQuantity.setText("Qty: " + (item.getQuantity() != null ? item.getQuantity() : 0));
            itemHolder.itemPrice.setText("Price: $" + (item.getPrice() != null ? item.getPrice() : "Not set"));

            itemHolder.editPrice.setOnClickListener(v -> {
                String purchaseGroupId = item.getGroupId();
                String dateTime = item.getDate();

                showEditPriceDialog(holder.itemView.getContext(), item, purchaseGroupId, dateTime);
            });
        } else if (holder instanceof TotalsViewHolder) {
            TotalsViewHolder totalsHolder = (TotalsViewHolder) holder;
            totalsHolder.totalsText.setText(item.getItemName());
        }
    }

    @Override
    public int getItemCount() {
        return purchases.size();
    }

    public static class GroupHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView groupName, dateLabel;

        public GroupHeaderViewHolder(View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.group_header_name);
            dateLabel = itemView.findViewById(R.id.date_label);
        }
    }

    private void showEditPriceDialog(Context context, HelperClass.ShoppingItem item, String purchaseGroupId, String dateTime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Price");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter new price");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newPrice = input.getText().toString();

            try {
                double price = Double.parseDouble(newPrice);
                item.setPrice(price);

                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(context, "User not logged in. Please log in.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String userEmail = "";

                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("expenses")
                        .child(purchaseGroupId)
                        .child(dateTime);

                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Traverse through all children of dateTime
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            String email = childSnapshot.getKey();
                            // Get all key-value pairs under the current child
                            DatabaseReference itemReference = FirebaseDatabase.getInstance().getReference("expenses")
                                    .child(purchaseGroupId)
                                    .child(dateTime)
                                    .child(email.replace(".", ","))
                                    .child("items")
                                    .child(item.getItemId());

                            itemReference.child("price").setValue(price)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(context, "Price updated successfully!", Toast.LENGTH_SHORT).show();
                                        notifyDataSetChanged();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(context, "Error updating price", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("Firebase", "Error fetching data", databaseError.toException());
                    }
                });

                if (userEmail == null) {
                    Toast.makeText(context, "Email not available.", Toast.LENGTH_SHORT).show();
                    return;
                }

            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid price entered", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemQuantity, itemPrice;
        ImageView editPrice;

        public ItemViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            itemPrice = itemView.findViewById(R.id.item_price);
            editPrice = itemView.findViewById(R.id.edit_price);
        }
    }

    public static class TotalsViewHolder extends RecyclerView.ViewHolder {
        TextView totalsText;

        public TotalsViewHolder(View itemView) {
            super(itemView);
            totalsText = itemView.findViewById(R.id.subtotal_value);
        }
    }
}
