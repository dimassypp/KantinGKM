package com.example.ratemy.kantingkm.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.models.Order;
import com.example.ratemy.kantingkm.models.OrderItem;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private static final String TAG = "CheckoutActivity";

    private TextView tvTotalPrice;
    private Button btnCheckout;
    private LinearLayout orderItemsContainer;
    private TextInputEditText etNotes;
    private String canteenId;
    private String canteenName;
    private ArrayList<OrderItem> selectedItems;
    private double totalPrice = 0;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get data from intent
        canteenId = getIntent().getStringExtra("canteenId");
        canteenName = getIntent().getStringExtra("canteenName");
        selectedItems = getIntent().getParcelableArrayListExtra("selectedItems");

        if (canteenId == null || selectedItems == null || selectedItems.isEmpty()) {
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        populateOrderItems();

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        btnCheckout.setOnClickListener(v -> showQRISDialog());
    }

    private void initializeViews() {
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnCheckout = findViewById(R.id.btnCheckout);
        orderItemsContainer = findViewById(R.id.orderItemsContainer);
        etNotes = findViewById(R.id.etNotes);

        // Set canteen name in toolbar if available
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        if (canteenName != null && toolbarTitle != null) {
            toolbarTitle.setText("Checkout - " + canteenName);
        }
    }

    private void populateOrderItems() {
        orderItemsContainer.removeAllViews();
        totalPrice = 0;

        for (OrderItem item : selectedItems) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_checkout, orderItemsContainer, false);

            TextView tvQuantity = itemView.findViewById(R.id.tvQuantity);
            TextView tvItemName = itemView.findViewById(R.id.tvItemName);
            TextView tvItemPrice = itemView.findViewById(R.id.tvItemPrice);

            tvQuantity.setText(String.format(Locale.getDefault(), "%dx", item.getQuantity()));
            tvItemName.setText(item.getName());
            double itemTotal = item.getPrice() * item.getQuantity();
            tvItemPrice.setText(String.format(Locale.getDefault(), "Rp. %,.0f", itemTotal));

            orderItemsContainer.addView(itemView);
            totalPrice += itemTotal;
        }

        tvTotalPrice.setText(String.format(Locale.getDefault(), "Rp. %,.0f", totalPrice));
    }

    private void showQRISDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_qris);
        dialog.setCancelable(false);

        Button btnDone = dialog.findViewById(R.id.btnSave);
        btnDone.setOnClickListener(v -> {
            dialog.dismiss();
            processOrder();
        });

        dialog.show();
    }

    private void processOrder() {
        // Validasi user sudah login
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validasi data yang diperlukan
        if (canteenId == null || canteenId.isEmpty()) {
            Log.e(TAG, "Canteen ID is null or empty");
            Toast.makeText(this, "Invalid canteen data", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedItems == null || selectedItems.isEmpty()) {
            Log.e(TAG, "No items selected");
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validasi setiap item
        for (OrderItem item : selectedItems) {
            if (item.getMenuId() == null || item.getMenuId().isEmpty()) {
                Log.e(TAG, "Invalid menu ID for item: " + item.getName());
                Toast.makeText(this, "Invalid item data", Toast.LENGTH_SHORT).show();
                return;
            }
            if (item.getQuantity() <= 0) {
                Log.e(TAG, "Invalid quantity for item: " + item.getName());
                Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String userId = mAuth.getCurrentUser().getUid();
        String orderId = mDatabase.child("orders").push().getKey();

        if (orderId == null) {
            Log.e(TAG, "Failed to generate order ID");
            Toast.makeText(this, "Failed to create order ID", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setCanteenId(canteenId);
        order.setItems(selectedItems);
        order.setTotalPrice(totalPrice);
        order.setStatus("pending");
        order.setCreatedAt(currentDate);
        order.setNotes(notes);

        Log.d(TAG, "Attempting to save order with ID: " + orderId);

        // Disable button untuk mencegah double submit
        btnCheckout.setEnabled(false);
        btnCheckout.setText("Processing...");

        mDatabase.child("orders").child(orderId).setValue(order)
                .addOnCompleteListener(task -> {
                    // Re-enable button
                    btnCheckout.setEnabled(true);
                    btnCheckout.setText("Checkout");

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Order saved successfully");
                        updateMenuStock();
                        showSuccessDialog();
                    } else {
                        Log.e(TAG, "Failed to save order", task.getException());
                        String errorMessage = "Failed to place order";
                        if (task.getException() != null) {
                            errorMessage += ": " + task.getException().getMessage();
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Re-enable button
                    btnCheckout.setEnabled(true);
                    btnCheckout.setText("Checkout");

                    Log.e(TAG, "Order submission failed", e);
                    Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateMenuStock() {
        for (OrderItem item : selectedItems) {
            if (item.getMenuId() == null || item.getMenuId().isEmpty()) {
                Log.w(TAG, "Skipping stock update for item with null/empty menuId: " + item.getName());
                continue;
            }

            DatabaseReference menuRef = mDatabase.child("menus").child(canteenId).child(item.getMenuId()).child("stock");
            menuRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Integer currentStock = task.getResult().getValue(Integer.class);
                    if (currentStock != null) {
                        int newStock = currentStock - item.getQuantity();
                        menuRef.setValue(Math.max(newStock, 0))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update stock for item: " + item.getName(), e));
                    } else {
                        Log.w(TAG, "Current stock is null for item: " + item.getName());
                    }
                } else {
                    Log.e(TAG, "Failed to get current stock for item: " + item.getName(), task.getException());
                }
            });
        }
    }

    private void showSuccessDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_success);
        dialog.setCancelable(false);

        new android.os.Handler().postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                dialog.dismiss();
                Intent intent = new Intent(this, OrderListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }, 3000);

        dialog.show();
    }
}