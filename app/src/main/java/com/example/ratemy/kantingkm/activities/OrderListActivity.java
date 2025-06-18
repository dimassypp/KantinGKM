package com.example.ratemy.kantingkm.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.adapters.OrderAdapter;
import com.example.ratemy.kantingkm.models.Order;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Map<String, String> canteenNamesMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeViews();
        loadCanteenNames(); // Load canteen names first
    }

    private void initializeViews() {
        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewOrders);
        if (recyclerView == null) {
            Toast.makeText(this, "RecyclerView not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(this, orderList);
        recyclerView.setAdapter(orderAdapter);

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_canteen) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_order) {
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
            bottomNavigationView.setSelectedItemId(R.id.nav_order);
        }
    }

    private void loadCanteenNames() {
        // Load canteen names
        mDatabase.child("canteens").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                canteenNamesMap.clear();
                for (DataSnapshot canteenSnapshot : snapshot.getChildren()) {
                    String canteenId = canteenSnapshot.getKey();
                    String canteenName = canteenSnapshot.child("name").getValue(String.class);
                    if (canteenId != null && canteenName != null) {
                        canteenNamesMap.put(canteenId, canteenName);
                    }
                }
                loadOrders();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OrderListActivity.this, "Failed to load canteen data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                loadOrders();
            }
        });
    }

    private void loadOrders() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();

        mDatabase.child("orders").orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Order order = dataSnapshot.getValue(Order.class);
                            if (order != null) {
                                order.setOrderId(dataSnapshot.getKey());

                                if (order.getCanteenId() != null && canteenNamesMap.containsKey(order.getCanteenId())) {
                                    order.setCanteenName(canteenNamesMap.get(order.getCanteenId()));
                                } else {
                                    order.setCanteenName("Unknown Canteen");
                                }

                                orderList.add(order);
                            }
                        }
                        orderAdapter.notifyDataSetChanged();

                        if (orderList.isEmpty()) {
                            Toast.makeText(OrderListActivity.this, "No orders found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(OrderListActivity.this, "Failed to load orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}