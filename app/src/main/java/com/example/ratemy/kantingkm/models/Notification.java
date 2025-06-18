// Notification.java - Model untuk notifikasi
package com.example.ratemy.kantingkm.models;

public class Notification {
    private String id;
    private String orderId;
    private String status;
    private String message;
    private long timestamp;
    private boolean read;
    private String type; // "order_status", "order_completed", etc.

    public Notification() {
        // Default constructor required for Firebase
    }

    public Notification(String orderId, String status, String message, long timestamp) {
        this.orderId = orderId;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.read = false;
        this.type = "order_status";
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFormattedTime() {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) return "Just now";
        if (diff < 3600000) return (diff / 60000) + " minutes ago";
        if (diff < 86400000) return (diff / 3600000) + " hours ago";
        return (diff / 86400000) + " days ago";
    }

    public String getStatusDisplayText() {
        switch (status.toLowerCase()) {
            case "pending": return "Order Placed";
            case "processing": return "Being Prepared";
            case "ready": return "Ready for Pickup";
            case "completed": return "Order Completed";
            case "canceled": return "Order Canceled";
            default: return status;
        }
    }
}