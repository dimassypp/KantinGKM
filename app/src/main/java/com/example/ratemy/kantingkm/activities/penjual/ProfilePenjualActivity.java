package com.example.ratemy.kantingkm.activities.penjual;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ratemy.kantingkm.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfilePenjualActivity extends AppCompatActivity {

    private static final String TAG = "ProfilePenjualActivity";
    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_PERMISSION = 1003;

    private TextInputEditText etCanteenName;
    private AutoCompleteTextView etCategory;
    private Button btnSave, btnLogout;
    private ImageView ivCanteenCover;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String canteenId;
    private String currentImageBase64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_penjual);

        initializeViews();
        setupFirebase();
        setupCategoryDropdown();
        setupClickListeners();
        setupBottomNavigation();

        // Load seller data
        loadSellerData();
    }

    private void initializeViews() {
        etCanteenName = findViewById(R.id.etCanteenName);
        etCategory = findViewById(R.id.etCategory);
        btnSave = findViewById(R.id.btnSave);
        btnLogout = findViewById(R.id.btnLogout);
        ivCanteenCover = findViewById(R.id.profileImage);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void setupCategoryDropdown() {
        String[] categories = {
                "Rice",
                "Noodles",
                "Beverages",
                "Snacks"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        etCategory.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Image click listener
        ivCanteenCover.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openGallery();
            } else {
                requestStoragePermission();
            }
        });

        // Save button
        btnSave.setOnClickListener(v -> saveProfile());

        // Logout button
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(ProfilePenjualActivity.this, com.example.ratemy.kantingkm.activities.LoginPenjualActivity.class));
            finish();
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_order_penjual) {
                startActivity(new Intent(ProfilePenjualActivity.this, OrderListPenjualActivity.class));
                return true;
            } else if (itemId == R.id.nav_menu) {
                startActivity(new Intent(ProfilePenjualActivity.this, MenuPenjualActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile_penjual) {
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_profile_penjual);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && requestCode == REQUEST_IMAGE_PICK) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    if (bitmap != null) {
                        bitmap = resizeBitmap(bitmap, 800, 800);
                        currentImageBase64 = bitmapToBase64(bitmap);
                        ivCanteenCover.setImageBitmap(bitmap);
                        Log.d(TAG, "Image converted to base64");
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found: " + e.getMessage());
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting base64 to bitmap: " + e.getMessage());
            return null;
        }
    }

    private void loadSellerData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        mDatabase.child("users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    createNewUserRecord(currentUser.getUid());
                    return;
                }

                canteenId = snapshot.child("canteenId").getValue(String.class);
                if (canteenId == null || canteenId.isEmpty()) {
                    createNewCanteenForUser(currentUser.getUid());
                } else {
                    loadCanteenData();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load user data: " + error.getMessage());
                Toast.makeText(ProfilePenjualActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewUserRecord(String userId) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", userId);
        user.put("role", "penjual");

        mDatabase.child("users").child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> createNewCanteenForUser(userId))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create user record", e);
                    Toast.makeText(this, "Failed to create user record", Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewCanteenForUser(String userId) {
        String newCanteenId = mDatabase.child("canteens").push().getKey();
        if (newCanteenId == null) {
            Toast.makeText(this, "Failed to create canteen", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> canteen = new HashMap<>();
        canteen.put("name", "New Canteen");
        canteen.put("category", "");
        canteen.put("ownerId", userId);
        canteen.put("imageUrl", "");

        // First create the canteen
        mDatabase.child("canteens").child(newCanteenId).setValue(canteen)
                .addOnSuccessListener(aVoid -> {
                    // Then update the user with the canteenId
                    mDatabase.child("users").child(userId).child("canteenId").setValue(newCanteenId)
                            .addOnSuccessListener(aVoid1 -> {
                                canteenId = newCanteenId;
                                loadCanteenData();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update user with canteenId", e);
                                Toast.makeText(this, "Failed to setup canteen", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create canteen", e);
                    Toast.makeText(this, "Failed to create canteen", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCanteenData() {
        if (canteenId == null || canteenId.isEmpty()) {
            Toast.makeText(this, "Canteen ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("canteens").child(canteenId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot canteenSnapshot) {
                if (!canteenSnapshot.exists()) {
                    Toast.makeText(ProfilePenjualActivity.this, "Canteen data not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                String canteenName = canteenSnapshot.child("name").getValue(String.class);
                String category = canteenSnapshot.child("category").getValue(String.class);
                String imageUrl = canteenSnapshot.child("imageUrl").getValue(String.class);

                if (canteenName != null) {
                    etCanteenName.setText(canteenName);
                }
                if (category != null) {
                    etCategory.setText(category, false);
                }
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    currentImageBase64 = imageUrl;
                    Bitmap bitmap = base64ToBitmap(imageUrl);
                    if (bitmap != null) {
                        ivCanteenCover.setImageBitmap(bitmap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load canteen data: " + error.getMessage());
                Toast.makeText(ProfilePenjualActivity.this, "Failed to load canteen data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String canteenName = etCanteenName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();

        if (canteenName.isEmpty()) {
            etCanteenName.setError("Canteen name cannot be empty");
            etCanteenName.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            etCategory.setError("Category cannot be empty");
            etCategory.requestFocus();
            return;
        }

        if (canteenId == null || canteenId.isEmpty()) {
            Toast.makeText(this, "Canteen ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", canteenName);
        updates.put("category", category);

        if (currentImageBase64 != null && !currentImageBase64.isEmpty()) {
            updates.put("imageUrl", currentImageBase64);
        }

        mDatabase.child("canteens").child(canteenId).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");

                    if (task.isSuccessful()) {
                        Toast.makeText(ProfilePenjualActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(ProfilePenjualActivity.this,
                                "Failed to update profile: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void redirectToLogin() {
        Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, com.example.ratemy.kantingkm.activities.LoginPenjualActivity.class));
        finish();
    }
}