package com.example.ratemy.kantingkm.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.adapters.MenuAdapter;
import com.example.ratemy.kantingkm.models.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MenuActivity extends AppCompatActivity {

    private static final String TAG = "MenuActivity";

    private RecyclerView recyclerView;
    private MenuAdapter menuAdapter;
    private List<MenuItem> menuList;
    private Button btnCheckout;
    private BottomNavigationView bottomNavigationView;
    private TextView toolbarTitle;
    private ImageButton backButton;

    private DatabaseReference mDatabase;
    private String canteenId;
    private String canteenName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Log.d(TAG, "MenuActivity onCreate called");

        Intent receivedIntent = getIntent();
        if (receivedIntent != null) {
            canteenId = receivedIntent.getStringExtra("canteenId");
            canteenName = receivedIntent.getStringExtra("canteenName");

            Log.d(TAG, "Received Intent: " + receivedIntent);
            Log.d(TAG, "Intent Action: " + receivedIntent.getAction());
            Log.d(TAG, "Intent Categories: " + receivedIntent.getCategories());
            Log.d(TAG, "All Intent Extras: " + receivedIntent.getExtras());
        } else {
            Log.e(TAG, "Received Intent is null");
        }

        Log.d(TAG, "Received canteenId: " + canteenId);
        Log.d(TAG, "Received canteenName: " + canteenName);

        if (canteenId == null || canteenId.isEmpty()) {
            Log.e(TAG, "canteenId is null or empty");
            Toast.makeText(this, "Error: No canteen selected", Toast.LENGTH_LONG).show();

            new Handler().postDelayed(() -> {
                Log.d(TAG, "Finishing activity due to invalid canteenId");
                finish();
            }, 2000);
            return;
        }

        try {
            initializeViews();
            setupFirebase();
            setupRecyclerView();
            setupCheckoutButton();
            setupBottomNavigation();

            loadCanteenName();
            loadMenuItems();

            Log.d(TAG, "MenuActivity setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in MenuActivity onCreate", e);
            Toast.makeText(this, "Error initializing menu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views");

        try {
            // Setup toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            backButton = findViewById(R.id.backButton);
            toolbarTitle = findViewById(R.id.toolbarTitle);

            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    Log.d(TAG, "Back button clicked");
                    finish();
                });
            } else {
                Log.w(TAG, "Back button not found in layout");
            }

            recyclerView = findViewById(R.id.recyclerViewMenu);
            btnCheckout = findViewById(R.id.btnCheckout);
            bottomNavigationView = findViewById(R.id.bottom_navigation2);

            if (recyclerView == null) {
                throw new RuntimeException("recyclerViewMenu not found in layout");
            }
            if (btnCheckout == null) {
                throw new RuntimeException("btnCheckout not found in layout");
            }
            if (bottomNavigationView == null) {
                throw new RuntimeException("bottom_navigation2 not found in layout");
            }
            if (toolbarTitle == null) {
                Log.w(TAG, "toolbarTitle not found in layout");
            }

            Log.d(TAG, "Views initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void setupFirebase() {
        try {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            Log.d(TAG, "Firebase Database initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase Database", e);
            Toast.makeText(this, "Firebase connection failed", Toast.LENGTH_SHORT).show();
            throw e;
        }
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView");

        try {
            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            menuList = new ArrayList<>();
            menuAdapter = new MenuAdapter(this, menuList, new MenuAdapter.OnQuantityChangeListener() {
                @Override
                public void onQuantityChanged() {
                    Log.d(TAG, "Quantity changed, updating checkout button");
                    updateCheckoutButton();
                }
            });
            recyclerView.setAdapter(menuAdapter);

            Log.d(TAG, "RecyclerView setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            throw e;
        }
    }

    private void setupCheckoutButton() {
        Log.d(TAG, "Setting up checkout button");

        btnCheckout.setOnClickListener(v -> {
            Log.d(TAG, "Checkout button clicked");

            try {
                List<com.example.ratemy.kantingkm.models.OrderItem> selectedItems = menuAdapter.getSelectedItems();
                if (selectedItems == null || selectedItems.isEmpty()) {
                    Log.w(TAG, "No items selected for checkout");
                    Toast.makeText(this, "Please select items first", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(MenuActivity.this, CheckoutActivity.class);
                intent.putExtra("canteenId", canteenId);

                ArrayList<com.example.ratemy.kantingkm.models.OrderItem> selectedItemsList = new ArrayList<>(selectedItems);
                intent.putParcelableArrayListExtra("selectedItems", selectedItemsList);

                Log.d(TAG, "Starting checkout with " + selectedItems.size() + " items");
                startActivity(intent);

            } catch (Exception e) {
                Log.e(TAG, "Error in checkout", e);
                Toast.makeText(this, "Error proceeding to checkout", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNavigation() {
        Log.d(TAG, "Setting up bottom navigation");

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Log.d(TAG, "Bottom navigation item selected: " + itemId);

            try {
                if (itemId == R.id.nav_canteen) {
                    Log.d(TAG, "nav_canteen selected - finishing current activity");
                    finish();
                    return true;

                } else if (itemId == R.id.nav_order) {
                    Log.d(TAG, "nav_order selected");
                    Intent intent = new Intent(MenuActivity.this, OrderListActivity.class);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_profile) {
                    Log.d(TAG, "nav_profile selected");
                    Intent intent = new Intent(MenuActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in bottom navigation", e);
                Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show();
            }

            return false;
        });
        Log.d(TAG, "Bottom navigation setup completed");
    }

    private void loadCanteenName() {
        Log.d(TAG, "Loading canteen name for ID: " + canteenId);

        if (mDatabase == null) {
            Log.e(TAG, "Database reference is null");
            if (toolbarTitle != null) {
                toolbarTitle.setText("Menu");
            }
            return;
        }

        mDatabase.child("canteens").child(canteenId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Canteen name snapshot exists: " + snapshot.exists());
                        Log.d(TAG, "Canteen name snapshot value: " + snapshot.getValue());

                        try {
                            if (snapshot.exists()) {
                                String retrievedCanteenName = snapshot.getValue(String.class);
                                Log.d(TAG, "Canteen name retrieved: " + retrievedCanteenName);

                                if (retrievedCanteenName != null && !retrievedCanteenName.trim().isEmpty()) {
                                    if (toolbarTitle != null) {
                                        toolbarTitle.setText(retrievedCanteenName);
                                    }
                                    canteenName = retrievedCanteenName; // Update local variable
                                } else {
                                    Log.w(TAG, "Canteen name is null or empty");
                                    if (toolbarTitle != null) {
                                        toolbarTitle.setText("Menu");
                                    }
                                }
                            } else {
                                Log.w(TAG, "Canteen not found in database");
                                if (toolbarTitle != null) {
                                    toolbarTitle.setText("Menu");
                                }
                                Toast.makeText(MenuActivity.this, "Canteen not found", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing canteen name", e);
                            if (toolbarTitle != null) {
                                toolbarTitle.setText("Menu");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error loading canteen name: " + error.getMessage());
                        Log.e(TAG, "Error code: " + error.getCode());
                        Log.e(TAG, "Error details: " + error.getDetails());

                        if (toolbarTitle != null) {
                            toolbarTitle.setText("Menu");
                        }

                        String errorMessage = "Failed to load canteen: " + error.getMessage();
                        Toast.makeText(MenuActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadMenuItems() {
        Log.d(TAG, "Loading menu items for canteen: " + canteenId);

        if (mDatabase == null) {
            Log.e(TAG, "Database reference is null");
            Toast.makeText(this, "Database connection error", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("menus").child(canteenId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Menu items snapshot exists: " + snapshot.exists());
                Log.d(TAG, "Menu items count: " + snapshot.getChildrenCount());

                try {
                    menuList.clear();

                    if (snapshot.exists() && snapshot.hasChildren()) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            try {
                                MenuItem menuItem = dataSnapshot.getValue(MenuItem.class);
                                if (menuItem != null) {
                                    menuItem.setId(dataSnapshot.getKey());
                                    if (menuItem.getImageBase64() != null && !menuItem.getImageBase64().isEmpty()) {
                                        Log.d(TAG, "Menu item " + menuItem.getName() + " has base64 image data");
                                    } else if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().isEmpty()) {
                                        Log.d(TAG, "Menu item " + menuItem.getName() + " has imageUrl: " + menuItem.getImageUrl());
                                    } else {
                                        Log.d(TAG, "Menu item " + menuItem.getName() + " has no image data");
                                    }

                                    menuList.add(menuItem);
                                    Log.d(TAG, "Added menu item: " + menuItem.getName() + " - Price: " + menuItem.getPrice());
                                } else {
                                    Log.w(TAG, "Menu item is null for key: " + dataSnapshot.getKey());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing menu item: " + dataSnapshot.getKey(), e);
                            }
                        }

                        Log.d(TAG, "Successfully loaded " + menuList.size() + " menu items");

                        if (menuList.isEmpty()) {
                            Toast.makeText(MenuActivity.this, "No menu items available for this canteen", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "No menu items found for canteen: " + canteenId);
                        Toast.makeText(MenuActivity.this, "No menu items available for this canteen", Toast.LENGTH_SHORT).show();
                    }

                    if (menuAdapter != null) {
                        menuAdapter.notifyDataSetChanged();
                        Log.d(TAG, "MenuAdapter notified of data changes");
                    } else {
                        Log.e(TAG, "MenuAdapter is null when trying to notify data changed");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error processing menu items", e);
                    Toast.makeText(MenuActivity.this, "Error loading menu items", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error loading menu items: " + error.getMessage());
                Log.e(TAG, "Error code: " + error.getCode());
                Log.e(TAG, "Error details: " + error.getDetails());

                String errorMessage = "Failed to load menu items: " + error.getMessage();
                Toast.makeText(MenuActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateCheckoutButton() {
        try {
            if (menuAdapter != null && menuAdapter.hasSelectedItems()) {
                btnCheckout.setVisibility(View.VISIBLE);
                bottomNavigationView.setVisibility(View.GONE);
                Log.d(TAG, "Checkout button shown - " + menuAdapter.getSelectedItems().size() + " items selected");
            } else {
                btnCheckout.setVisibility(View.GONE);
                bottomNavigationView.setVisibility(View.VISIBLE);
                Log.d(TAG, "Checkout button hidden - no items selected");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating checkout button", e);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed");
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MenuActivity resumed with canteenId: " + canteenId);

        if (canteenId == null || canteenId.isEmpty()) {
            Log.e(TAG, "canteenId is null on resume, finishing activity");
            Toast.makeText(this, "Invalid canteen data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        updateCheckoutButton();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MenuActivity paused");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MenuActivity started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MenuActivity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MenuActivity destroyed");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "Saving instance state");

        // Save data
        if (canteenId != null) {
            outState.putString("canteenId", canteenId);
        }
        if (canteenName != null) {
            outState.putString("canteenName", canteenName);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "Restoring instance state");

        // Restore data
        if (savedInstanceState.containsKey("canteenId")) {
            canteenId = savedInstanceState.getString("canteenId");
            Log.d(TAG, "Restored canteenId: " + canteenId);
        }
        if (savedInstanceState.containsKey("canteenName")) {
            canteenName = savedInstanceState.getString("canteenName");
            Log.d(TAG, "Restored canteenName: " + canteenName);
        }
    }
}