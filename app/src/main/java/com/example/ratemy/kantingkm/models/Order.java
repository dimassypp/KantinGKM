package com.example.ratemy.kantingkm.models;

import java.util.List;

public class Order {
    private String orderId;
    private String userId;
    private String canteenId;
    private String canteenName;
    private List<OrderItem> items;
    private double totalPrice;
    private String status;
    private String notes;
    private String createdAt;

    // Default constructor required for Firebase
    public Order() {
        // Initialize default values
        this.status = "pending";
    }

    // Constructor with parameters
    public Order(String userId, String canteenId, List<OrderItem> items, double totalPrice, String status, String notes, String createdAt) {
        this.userId = userId;
        this.canteenId = canteenId;
        this.items = items;
        this.totalPrice = totalPrice;
        this.status = status != null ? status : "pending";
        this.notes = notes;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCanteenId() {
        return canteenId;
    }

    public void setCanteenId(String canteenId) {
        this.canteenId = canteenId;
    }

    public String getCanteenName() {
        return canteenName;
    }

    public void setCanteenName(String canteenName) {
        this.canteenName = canteenName;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status != null ? status : "pending";
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "pending";
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}