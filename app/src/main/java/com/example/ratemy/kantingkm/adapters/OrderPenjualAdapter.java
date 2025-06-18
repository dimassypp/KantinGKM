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

public class OrderPenjualAdapter extends RecyclerView.Adapter<OrderPenjualAdapter.OrderPenjualViewHolder> {
    private Context context;
    private List<Order> orderList;

    public OrderPenjualAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderPenjualViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_penjual, parent, false);
        return new OrderPenjualViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderPenjualViewHolder holder, int position) {
        Order order = orderList.get(position);
        if (order == null) return;

        holder.tvOrderId.setText(order.getOrderId() != null ? "#" + order.getOrderId().substring(0, 6) : "");
        holder.tvTanggal.setText(order.getCreatedAt() != null ? order.getCreatedAt() : "");

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

        // Set total price in the status field (as per original OrderPenjualAdapter mapping)
        holder.tvTotal.setText(String.format("Rp. %,.0f", order.getTotalPrice()));
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public static class OrderPenjualViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvTanggal, tvItem1, tvItem2, tvTotal;
        ImageView ivMakanan;

        public OrderPenjualViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.textViewNamaToko);
            tvTanggal = itemView.findViewById(R.id.textViewTanggal);
            tvItem1 = itemView.findViewById(R.id.textViewItem1);
            tvItem2 = itemView.findViewById(R.id.textViewItem2);
            tvTotal = itemView.findViewById(R.id.textViewStatus);
            ivMakanan = itemView.findViewById(R.id.imageViewMakanan);
        }
    }
}