package com.example.ratemy.kantingkm.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.models.Order;
import com.example.ratemy.kantingkm.models.OrderItem;

import java.util.List;

public class ActiveOrderAdapter extends RecyclerView.Adapter<ActiveOrderAdapter.ActiveOrderViewHolder> {

    public interface OnOrderActionListener {
        void onCancelOrder(Order order);
        void onProcessOrder(Order order);
        void onReadyOrder(Order order);
        void onCompleteOrder(Order order);
    }

    private Context context;
    private List<Order> orderList;
    private OnOrderActionListener listener;

    public ActiveOrderAdapter(Context context, List<Order> orderList, OnOrderActionListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActiveOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_active_order_penjual, parent, false);
        return new ActiveOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveOrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        if (order == null) return;

        // Set order details
        holder.tvOrderTitle.setText(order.getOrderId() != null ? "Order #" + order.getOrderId().substring(0, Math.min(6, order.getOrderId().length())) : "New Order");
        holder.tvTime.setText(order.getCreatedAt() != null ? order.getCreatedAt().split(" ")[1] : "");

        // Set status with proper formatting
        String status = order.getStatus() != null ? order.getStatus() : "pending";
        holder.tvStatus.setText(capitalizeFirst(status));

        // Clear previous items
        holder.tvItem1.setText("");
        holder.tvItem2.setText("");
        holder.tvItem1.setVisibility(View.GONE);
        holder.tvItem2.setVisibility(View.GONE);

        // Set order items
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            if (order.getItems().size() > 0) {
                OrderItem firstItem = order.getItems().get(0);
                holder.tvItem1.setText(firstItem.getQuantity() + "x " + firstItem.getName());
                holder.tvItem1.setVisibility(View.VISIBLE);
            }
            if (order.getItems().size() > 1) {
                OrderItem secondItem = order.getItems().get(1);
                holder.tvItem2.setText(secondItem.getQuantity() + "x " + secondItem.getName());
                holder.tvItem2.setVisibility(View.VISIBLE);
            }
            if (order.getItems().size() > 2) {
                String currentText = holder.tvItem2.getText().toString();
                holder.tvItem2.setText(currentText + " (+" + (order.getItems().size() - 2) + " more)");
            }
        }

        // Set button actions
        holder.btnCancel.setOnClickListener(v -> {
            if (listener != null) listener.onCancelOrder(order);
        });

        holder.btnProcess.setOnClickListener(v -> {
            if (listener != null) listener.onProcessOrder(order);
        });

        holder.btnReady.setOnClickListener(v -> {
            if (listener != null) listener.onReadyOrder(order);
        });

        holder.btnComplete.setOnClickListener(v -> {
            if (listener != null) listener.onCompleteOrder(order);
        });

        // Update button states based on current status
        updateButtonStates(holder, status);
    }

    private void updateButtonStates(ActiveOrderViewHolder holder, String status) {
        // First make all buttons visible and reset states
        holder.btnCancel.setVisibility(View.VISIBLE);
        holder.btnProcess.setVisibility(View.VISIBLE);
        holder.btnReady.setVisibility(View.VISIBLE);
        holder.btnComplete.setVisibility(View.VISIBLE);

        // Reset all buttons to enabled
        holder.btnCancel.setEnabled(true);
        holder.btnProcess.setEnabled(true);
        holder.btnReady.setEnabled(true);
        holder.btnComplete.setEnabled(true);

        // Reset alpha for visual feedback
        holder.btnCancel.setAlpha(1.0f);
        holder.btnProcess.setAlpha(1.0f);
        holder.btnReady.setAlpha(1.0f);
        holder.btnComplete.setAlpha(1.0f);

        if (status == null) status = "pending";

        switch (status.toLowerCase()) {
            case "pending":
                // Only Process and Cancel buttons should be active
                holder.btnReady.setEnabled(false);
                holder.btnReady.setAlpha(0.5f);
                holder.btnComplete.setEnabled(false);
                holder.btnComplete.setAlpha(0.5f);
                break;

            case "processing":
                // Only Ready and Cancel buttons should be active
                holder.btnProcess.setEnabled(false);
                holder.btnProcess.setAlpha(0.5f);
                holder.btnComplete.setEnabled(false);
                holder.btnComplete.setAlpha(0.5f);
                break;

            case "ready":
                // Only Complete button should be active
                holder.btnCancel.setEnabled(false);
                holder.btnCancel.setAlpha(0.5f);
                holder.btnProcess.setEnabled(false);
                holder.btnProcess.setAlpha(0.5f);
                holder.btnReady.setEnabled(false);
                holder.btnReady.setAlpha(0.5f);
                break;

            case "completed":
            case "canceled":
                // All buttons disabled
                holder.btnCancel.setEnabled(false);
                holder.btnCancel.setAlpha(0.5f);
                holder.btnProcess.setEnabled(false);
                holder.btnProcess.setAlpha(0.5f);
                holder.btnReady.setEnabled(false);
                holder.btnReady.setAlpha(0.5f);
                holder.btnComplete.setEnabled(false);
                holder.btnComplete.setAlpha(0.5f);
                break;
        }
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public void updateOrder(Order updatedOrder) {
        for (int i = 0; i < orderList.size(); i++) {
            if (orderList.get(i).getOrderId().equals(updatedOrder.getOrderId())) {
                orderList.set(i, updatedOrder);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public static class ActiveOrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderTitle, tvStatus, tvTime, tvItem1, tvItem2;
        Button btnCancel, btnProcess, btnReady, btnComplete;

        public ActiveOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderTitle = itemView.findViewById(R.id.textViewOrderTitle);
            tvStatus = itemView.findViewById(R.id.textViewStatus);
            tvTime = itemView.findViewById(R.id.textViewTime);
            tvItem1 = itemView.findViewById(R.id.textViewItem1);
            tvItem2 = itemView.findViewById(R.id.textViewItem2);
            btnCancel = itemView.findViewById(R.id.buttonCancel);
            btnProcess = itemView.findViewById(R.id.buttonProcess);
            btnReady = itemView.findViewById(R.id.buttonReady);
            btnComplete = itemView.findViewById(R.id.buttonComplete);
        }
    }
}