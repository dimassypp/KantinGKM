package com.example.ratemy.kantingkm.models;

public class MenuItem {
    private String id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String imageUrl;
    private String imageBase64; // Tambahan field untuk base64
    private String canteenId;
    private boolean available; // Tambahan field untuk status ketersediaan

    // Constructors
    public MenuItem() {}

    public MenuItem(String name, String description, double price, int stock,
                    String imageBase64, String canteenId, boolean available) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageBase64 = imageBase64;
        this.canteenId = canteenId;
        this.available = available;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // Methods untuk imageBase64
    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getCanteenId() {
        return canteenId;
    }

    public void setCanteenId(String canteenId) {
        this.canteenId = canteenId;
    }

    // Methods untuk available status
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "MenuItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", imageUrl='" + imageUrl + '\'' +
                ", imageBase64='" + (imageBase64 != null ? "***" : "null") + '\'' +
                ", canteenId='" + canteenId + '\'' +
                ", available=" + available +
                '}';
    }
}