package com.example.ratemy.kantingkm.activities.penjual;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.adapters.OrderPenjualAdapter;
import com.example.ratemy.kantingkm.models.Order;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class OrderListPenjualActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrderPenjualAdapter orderAdapter;
    private List<Order> orderList;
    private String canteenId;
    private ImageButton backButton;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list_penjual);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get current user's canteen
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("canteenId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                canteenId = snapshot.getValue(String.class);
                if (canteenId != null) {
                    initializeViews();
                    loadOrders();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OrderListPenjualActivity.this, "Failed to load canteen data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeViews() {
        // Initialize Back Button
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OrderListPenjualActivity.this, ActiveOrderActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewOrders);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        orderList = new ArrayList<>();
        orderAdapter = new OrderPenjualAdapter(this, orderList);
        recyclerView.setAdapter(orderAdapter);

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_order_penjual) {
                startActivity(new Intent(this, ActiveOrderActivity.class));
                finish();
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
    }

    private void loadOrders() {
        mDatabase.child("orders").orderByChild("canteenId").equalTo(canteenId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Order order = dataSnapshot.getValue(Order.class);
                            if (order != null) {
                                order.setOrderId(dataSnapshot.getKey());
                                orderList.add(order);
                            }
                        }
                        orderAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(OrderListPenjualActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}