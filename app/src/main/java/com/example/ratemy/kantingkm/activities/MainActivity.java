package com.example.ratemy.kantingkm.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.adapters.CanteenAdapter;
import com.example.ratemy.kantingkm.models.Canteen;
import com.example.ratemy.kantingkm.models.Notification;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerViewCanteen;
    private CanteenAdapter canteenAdapter;
    private List<Canteen> canteenList;
    private BottomNavigationView bottomNavigationView;
    private ImageView imageViewNotification;
    private TextView notificationBadge; // Badge untuk notifikasi

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private ValueEventListener notificationListener; // Listener untuk notifikasi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity created");

        initializeViews();
        setupFirebase();
        setupRecyclerView();
        setupBottomNavigation();
        setupNotificationButton();

        // Authenticate first, then load canteens
        authenticateAndLoadData();
    }

    private void initializeViews() {
        recyclerViewCanteen = findViewById(R.id.recyclerViewCanteen);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        imageViewNotification = findViewById(R.id.imageViewNotification);
        notificationBadge = findViewById(R.id.notificationBadge); // Initialize badge
    }

    private void setupFirebase() {
        try {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            mAuth = FirebaseAuth.getInstance();
            Log.d(TAG, "Firebase Database and Auth initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase", e);
            Toast.makeText(this, "Firebase connection failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNotificationButton() {
        imageViewNotification.setOnClickListener(v -> {
            Log.d(TAG, "Notification button clicked");
            try {
                Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting NotificationActivity", e);
                Toast.makeText(MainActivity.this, "Error opening notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void authenticateAndLoadData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already authenticated: " + currentUser.getUid());
            loadCanteens();
            setupNotificationBadge(); // Setup badge setelah user authenticated
        } else {
            mAuth.signInAnonymously()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInAnonymously:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Log.d(TAG, "Authenticated as: " + user.getUid());
                            }
                            loadCanteens();
                            setupNotificationBadge(); // Setup badge setelah user authenticated
                        } else {
                            Log.w(TAG, "signInAnonymously:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed: " +
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void setupNotificationBadge() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated when setting up notification badge");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Setting up notification badge for user: " + userId);

        // Remove existing listener if any
        if (notificationListener != null) {
            mDatabase.child("notifications").child(userId).removeEventListener(notificationListener);
        }

        // Create new listener for real-time badge updates
        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unreadCount = 0;
                Log.d(TAG, "Notification snapshot exists: " + snapshot.exists());
                Log.d(TAG, "Notification count: " + snapshot.getChildrenCount());

                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        try {
                            Notification notification = dataSnapshot.getValue(Notification.class);
                            if (notification != null && !notification.isRead()) {
                                unreadCount++;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification: " + dataSnapshot.getKey(), e);
                        }
                    }
                }

                Log.d(TAG, "Unread notifications count: " + unreadCount);
                updateNotificationBadge(unreadCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load notification count: " + error.getMessage());
            }
        };

        // Attach listener
        mDatabase.child("notifications").child(userId).addValueEventListener(notificationListener);
    }

    private void updateNotificationBadge(int count) {
        runOnUiThread(() -> {
            if (count > 0) {
                notificationBadge.setText(String.valueOf(count > 99 ? "99+" : count));
                notificationBadge.setVisibility(android.view.View.VISIBLE);
                Log.d(TAG, "Badge updated: " + count + " unread notifications");
            } else {
                notificationBadge.setVisibility(android.view.View.GONE);
                Log.d(TAG, "Badge hidden: no unread notifications");
            }
        });
    }

    private void setupRecyclerView() {
        recyclerViewCanteen.setHasFixedSize(true);
        recyclerViewCanteen.setLayoutManager(new LinearLayoutManager(this));

        canteenList = new ArrayList<>();
        canteenAdapter = new CanteenAdapter(this, canteenList, new CanteenAdapter.OnCanteenClickListener() {
            @Override
            public void onCanteenClick(Canteen canteen) {
                Log.d(TAG, "Canteen clicked: " + canteen.getName() + " (ID: " + canteen.getId() + ")");

                if (canteen == null) {
                    Log.e(TAG, "Canteen object is null");
                    Toast.makeText(MainActivity.this, "Error: Invalid canteen data", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (canteen.getId() == null || canteen.getId().isEmpty()) {
                    Log.e(TAG, "Canteen ID is null or empty");
                    Toast.makeText(MainActivity.this, "Error: Invalid canteen ID", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                    intent.putExtra("canteenId", canteen.getId());
                    intent.putExtra("canteenName", canteen.getName());

                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    Log.d(TAG, "Starting MenuActivity with canteenId: " + canteen.getId());
                    Log.d(TAG, "Intent extras: " + intent.getExtras());

                    startActivity(intent);

                } catch (Exception e) {
                    Log.e(TAG, "Error starting MenuActivity", e);
                    Toast.makeText(MainActivity.this, "Error opening menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        recyclerViewCanteen.setAdapter(canteenAdapter);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Log.d(TAG, "Bottom navigation item selected: " + itemId);

            if (itemId == R.id.nav_canteen) {
                Log.d(TAG, "Already on MainActivity");
                return true;
            } else if (itemId == R.id.nav_order) {
                Log.d(TAG, "Navigating to OrderListActivity");
                try {
                    Intent intent = new Intent(MainActivity.this, OrderListActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting OrderListActivity", e);
                    Toast.makeText(MainActivity.this, "Error opening orders", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.nav_profile) {
                Log.d(TAG, "Navigating to ProfileActivity");
                try {
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting ProfileActivity", e);
                    Toast.makeText(MainActivity.this, "Error opening profile", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_canteen);
    }

    private void loadCanteens() {
        Log.d(TAG, "Loading canteens from Firebase");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated when trying to load canteens");
            Toast.makeText(this, "Please wait for authentication to complete", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "User authenticated: " + currentUser.getUid());

        mDatabase.child("canteens").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Canteens snapshot exists: " + snapshot.exists());
                Log.d(TAG, "Canteens count: " + snapshot.getChildrenCount());

                canteenList.clear();

                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        try {
                            Canteen canteen = dataSnapshot.getValue(Canteen.class);
                            if (canteen != null) {
                                canteen.setId(dataSnapshot.getKey());

                                // FIXED: Konsisten dengan ProfilePenjualActivity - imageUrl berisi base64
                                String imageData = dataSnapshot.child("imageUrl").getValue(String.class);

                                // Validasi dan set image data
                                if (imageData != null && !imageData.trim().isEmpty()) {
                                    canteen.setImageUrl(imageData);
                                    Log.d(TAG, "Image data loaded for canteen: " + canteen.getName() +
                                            " (length: " + imageData.length() + ")");
                                } else {
                                    canteen.setImageUrl(""); // Set empty string untuk konsistensi
                                    Log.d(TAG, "No image data for canteen: " + canteen.getName());
                                }

                                canteenList.add(canteen);
                                Log.d(TAG, "Added canteen: " + canteen.getName() + " (ID: " + canteen.getId() + ")");
                            } else {
                                Log.w(TAG, "Canteen is null for key: " + dataSnapshot.getKey());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing canteen: " + dataSnapshot.getKey(), e);
                        }
                    }

                    Log.d(TAG, "Successfully loaded " + canteenList.size() + " canteens");

                    if (canteenList.isEmpty()) {
                        Log.w(TAG, "Canteen list is empty after processing");
                        Toast.makeText(MainActivity.this, "No canteens available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "No canteens found in database");
                    Toast.makeText(MainActivity.this, "No canteens available", Toast.LENGTH_SHORT).show();
                }

                canteenAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error loading canteens: " + error.getMessage());
                Log.e(TAG, "Error code: " + error.getCode());
                Log.e(TAG, "Error details: " + error.getDetails());

                // Provide more specific error messages based on error code
                String errorMessage;
                switch (error.getCode()) {
                    case DatabaseError.PERMISSION_DENIED:
                        errorMessage = "Access denied. Please check authentication.";
                        break;
                    case DatabaseError.NETWORK_ERROR:
                        errorMessage = "Network error. Please check your connection.";
                        break;
                    case DatabaseError.UNAVAILABLE:
                        errorMessage = "Database service is temporarily unavailable.";
                        break;
                    default:
                        errorMessage = "Failed to load canteens: " + error.getMessage();
                }

                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Method for debugging
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult called - RequestCode: " + requestCode + ", ResultCode: " + resultCode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity resumed");

        if (mAuth != null && mAuth.getCurrentUser() != null) {
            Log.d(TAG, "Data will be updated automatically via ValueEventListener");
            if (notificationListener == null) {
                setupNotificationBadge();
            }
        }
        if (!isFinishing() && !isDestroyed()) {
            bottomNavigationView.setSelectedItemId(R.id.nav_canteen);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity paused");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");

        if (notificationListener != null && mAuth != null && mAuth.getCurrentUser() != null) {
            mDatabase.child("notifications").child(mAuth.getCurrentUser().getUid())
                    .removeEventListener(notificationListener);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed in MainActivity");
        super.onBackPressed();
    }
}