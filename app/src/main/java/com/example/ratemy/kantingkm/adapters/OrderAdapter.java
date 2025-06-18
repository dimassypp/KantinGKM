package com.example.ratemy.kantingkm.adapters;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.models.Order;
import com.example.ratemy.kantingkm.models.OrderItem;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private Context context;
    private List<Order> orderList;

    public OrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        if (order == null) return;

        holder.tvOrderId.setText(order.getOrderId() != null ? "#" + order.getOrderId().substring(0, 6) : "");
        holder.tvTanggal.setText(order.getCreatedAt() != null ? order.getCreatedAt() : "");

        // Gunakan canteenName jika ada, jika tidak ada gunakan fallback
        if (order.getCanteenName() != null && !order.getCanteenName().isEmpty()) {
            holder.tvNamaToko.setText(order.getCanteenName());
        } else {
            holder.tvNamaToko.setText(order.getCanteenId() != null ? "Kantin " + order.getCanteenId() : "Unknown Canteen");
        }

        // Clear previous items
        holder.tvItem1.setText("");
        holder.tvItem2.setText("");

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            if (order.getItems().size() > 0) {
                OrderItem item = order.getItems().get(0);
                holder.tvItem1.setText(item.getQuantity() + "x " + item.getName());
            }
            if (order.getItems().size() > 1) {
                OrderItem item = order.getItems().get(1);
                holder.tvItem2.setText(item.getQuantity() + "x " + item.getName());
            }
        }

        // Set status with appropriate background
        String status = order.getStatus() != null ? order.getStatus() : "pending";
        holder.tvStatus.setText(status);
        switch (status.toLowerCase()) {
            case "processing":
                holder.tvStatus.setBackgroundResource(R.drawable.status_processing_2);
                break;
            case "ready":
                holder.tvStatus.setBackgroundResource(R.drawable.status_ready);
                break;
            case "completed":
                holder.tvStatus.setBackgroundResource(R.drawable.status_completed_2);
                break;
            case "canceled":
                holder.tvStatus.setBackgroundResource(R.drawable.status_canceled);
                break;
            default:
                holder.tvStatus.setBackgroundResource(R.drawable.status_pending);
        }
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvNamaToko, tvTanggal, tvOrderId, tvItem1, tvItem2, tvStatus;
        ImageView ivMakanan;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNamaToko = itemView.findViewById(R.id.textViewNamaToko);
            tvTanggal = itemView.findViewById(R.id.textViewTanggal);
            tvOrderId = itemView.findViewById(R.id.textViewOrderId);
            tvItem1 = itemView.findViewById(R.id.textViewItem1);
            tvItem2 = itemView.findViewById(R.id.textViewItem2);
            tvStatus = itemView.findViewById(R.id.textViewStatus);
            ivMakanan = itemView.findViewById(R.id.imageViewMakanan);
        }
    }
}