package com.example.ratemy.kantingkm.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.models.Notification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private Context context;
    private List<Notification> notificationList;

    public NotificationAdapter(Context context, List<Notification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.textMessage.setText(notification.getMessage());
        holder.textStatus.setText(notification.getStatusDisplayText());
        holder.textTime.setText(notification.getFormattedTime());
        holder.textOrderId.setText("Order #" + notification.getOrderId().substring(0, 8));

        if (!notification.isRead()) {
            holder.textMessage.setTypeface(null, Typeface.BOLD);
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.darker_gray));
        } else {
            holder.textMessage.setTypeface(null, Typeface.NORMAL);
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        int statusColor;
        switch (notification.getStatus().toLowerCase()) {
            case "pending":
                statusColor = context.getResources().getColor(R.color.darker_gray);
                break;
            case "processing":
                statusColor = context.getResources().getColor(R.color.buttonWarning);
                break;
            case "ready":
                statusColor = context.getResources().getColor(R.color.buttonPrimary);
                break;
            case "completed":
                statusColor = context.getResources().getColor(R.color.buttonSuccess);
                break;
            case "canceled":
                statusColor = context.getResources().getColor(R.color.buttonDanger);
                break;
            default:
                statusColor = context.getResources().getColor(R.color.white);
                break;
        }
        holder.textStatus.setTextColor(statusColor);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textStatus, textTime, textOrderId;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textNotificationMessage);
            textStatus = itemView.findViewById(R.id.textNotificationStatus);
            textTime = itemView.findViewById(R.id.textNotificationTime);
            textOrderId = itemView.findViewById(R.id.textNotificationOrderId);
        }
    }
}