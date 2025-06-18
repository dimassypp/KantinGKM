package com.example.ratemy.kantingkm.models;

import android.os.Parcel;
import android.os.Parcelable;

public class OrderItem implements Parcelable {
    private String menuId;
    private String name;
    private int quantity;
    private double price;

    public OrderItem() {}

    protected OrderItem(Parcel in) {
        menuId = in.readString();
        name = in.readString();
        quantity = in.readInt();
        price = in.readDouble();
    }

    public static final Creator<OrderItem> CREATOR = new Creator<OrderItem>() {
        @Override
        public OrderItem createFromParcel(Parcel in) {
            return new OrderItem(in);
        }

        @Override
        public OrderItem[] newArray(int size) {
            return new OrderItem[size];
        }
    };

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(menuId);
        dest.writeString(name);
        dest.writeInt(quantity);
        dest.writeDouble(price);
    }
}