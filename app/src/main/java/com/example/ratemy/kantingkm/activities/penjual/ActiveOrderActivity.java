package com.example.ratemy.kantingkm.activities.penjual;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.adapters.ActiveOrderAdapter;
import com.example.ratemy.kantingkm.models.Order;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ActiveOrderAdapter activeOrderAdapter;
    private List<Order> activeOrderList;
    private String canteenId;
    private ImageView btnHistory;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_order_penjual);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get current user's canteen
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("canteenId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                canteenId = snapshot.getValue(String.class);
                if (canteenId != null) {
                    initializeViews();
                    loadActiveOrders();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ActiveOrderActivity.this, "Failed to load canteen data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeViews() {
        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewActiveOrders);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize History ImageView
        btnHistory = findViewById(R.id.imageViewHistory);
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Order History (OrderListPenjualActivity)
                Intent intent = new Intent(ActiveOrderActivity.this, OrderListPenjualActivity.class);
                startActivity(intent);
            }
        });

        activeOrderList = new ArrayList<>();
        activeOrderAdapter = new ActiveOrderAdapter(this, activeOrderList, new ActiveOrderAdapter.OnOrderActionListener() {
            @Override
            public void onCancelOrder(Order order) {
                updateOrderStatus(order.getOrderId(), "canceled");
            }

            @Override
            public void onProcessOrder(Order order) {
                updateOrderStatus(order.getOrderId(), "processing");
            }

            @Override
            public void onReadyOrder(Order order) {
                updateOrderStatus(order.getOrderId(), "ready");
            }

            @Override
            public void onCompleteOrder(Order order) {
                updateOrderStatus(order.getOrderId(), "completed");
            }
        });
        recyclerView.setAdapter(activeOrderAdapter);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationViewPenjual);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_order_penjual) {
                return true;
            } else if (itemId == R.id.nav_menu) {
                startActivity(new Intent(this, MenuPenjualActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile_penjual) {
                startActivity(new Intent(this, ProfilePenjualActivity.class));
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_order_penjual);
    }

    private void loadActiveOrders() {
        mDatabase.child("orders").orderByChild("canteenId").equalTo(canteenId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        activeOrderList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Order order = dataSnapshot.getValue(Order.class);
                            if (order != null && !order.getStatus().equals("completed") && !order.getStatus().equals("canceled")) {
                                order.setOrderId(dataSnapshot.getKey());
                                activeOrderList.add(order);
                            }
                        }
                        activeOrderAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ActiveOrderActivity.this, "Failed to load active orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        mDatabase.child("orders").child(orderId).child("status").setValue(newStatus)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ActiveOrderActivity.this, "Order status updated", Toast.LENGTH_SHORT).show();
                        // Send notification to buyer
                        sendStatusUpdateNotification(orderId, newStatus);
                    } else {
                        Toast.makeText(ActiveOrderActivity.this, "Failed to update order status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendStatusUpdateNotification(String orderId, String newStatus) {
        // Get buyer ID first
        mDatabase.child("orders").child(orderId).child("userId")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String buyerId = snapshot.getValue(String.class);
                        if (buyerId != null) {
                            // Create notification message
                            String message = getNotificationMessage(newStatus);

                            // Create notification object
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("orderId", orderId);
                            notification.put("status", newStatus);
                            notification.put("message", message);
                            notification.put("timestamp", System.currentTimeMillis());
                            notification.put("read", false);
                            notification.put("type", "order_status");

                            mDatabase.child("notifications").child(buyerId)
                                    .push().setValue(notification)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(ActiveOrderActivity.this,
                                                    "Buyer will be notified", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    private String getNotificationMessage(String status) {
        switch (status.toLowerCase()) {
            case "processing":
                return "Great! Your order is now being prepared by the seller.";
            case "ready":
                return "Your order is ready for pickup! Please come to collect it.";
            case "completed":
                return "Order completed! Thank you for your purchase.";
            case "canceled":
                return "Sorry, your order has been canceled by the seller.";
            default:
                return "Your order status has been updated to: " + status;
        }
    }
}