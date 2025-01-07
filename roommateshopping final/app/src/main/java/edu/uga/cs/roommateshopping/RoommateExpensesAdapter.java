package edu.uga.cs.roommateshopping;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoommateExpensesAdapter extends RecyclerView.Adapter<RoommateExpensesAdapter.ViewHolder> {
    private Map<String, Double> roommateSpendingMap;
    private double totalSpent;
    private double averageSpent;
    private List<String> roommates;

    public RoommateExpensesAdapter(Map<String, Double> roommateSpendingMap) {
        this.roommateSpendingMap = roommateSpendingMap;
        this.roommates = new ArrayList<>();
    }

    public void updateData(Map<String, Double> newSpendingMap, double totalSpent, double averageSpent) {
        this.roommateSpendingMap = newSpendingMap;
        this.totalSpent = totalSpent;
        this.averageSpent = averageSpent;
        this.roommates = new ArrayList<>(newSpendingMap.keySet());
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_roommate_expenses, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String roommate = roommates.get(position);
        double spent = roommateSpendingMap.getOrDefault(roommate, 0.0);
        double difference = spent - averageSpent;

        holder.roommateName.setText(roommate);
        holder.roommateSpent.setText(String.format("Spent: $%.2f", spent));
        holder.roommateAverage.setText(String.format("Average: $%.2f", averageSpent));
        holder.roommateDifference.setText(String.format("Difference: $%.2f", difference));
    }

    @Override
    public int getItemCount() {
        return roommates.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView roommateName, roommateSpent, roommateAverage, roommateDifference;

        public ViewHolder(View view) {
            super(view);
            roommateName = view.findViewById(R.id.roommate_name);
            roommateSpent = view.findViewById(R.id.roommate_spent);
            roommateAverage = view.findViewById(R.id.roommate_average);
            roommateDifference = view.findViewById(R.id.roommate_difference);
        }
    }
}
