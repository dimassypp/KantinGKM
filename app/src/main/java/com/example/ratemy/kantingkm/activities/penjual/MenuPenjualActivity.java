package com.example.ratemy.kantingkm.activities.penjual;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Window;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.adapters.MenuPenjualAdapter;
import com.example.ratemy.kantingkm.models.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuPenjualActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MenuPenjualAdapter menuAdapter;
    private List<MenuItem> menuList;
    private ImageButton btnAddMenu;

    private DatabaseReference mDatabase;
    private String canteenId;

    // Image upload variables
    private String currentImageBase64 = "";
    private ImageView currentImageView;
    private LinearLayout currentUploadPlaceholder;

    // Activity result launchers
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_penjual);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize activity result launchers
        initializeActivityResultLaunchers();

        // Get current user's canteen
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("canteenId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                canteenId = snapshot.getValue(String.class);
                if (canteenId != null) {
                    loadMenuItems();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MenuPenjualActivity.this, "Failed to load canteen data", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewMenu);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        menuList = new ArrayList<>();
        menuAdapter = new MenuPenjualAdapter(this, menuList, new MenuPenjualAdapter.OnMenuActionListener() {
            @Override
            public void onEditMenu(MenuItem menuItem) {
                showEditMenuDialog(menuItem);
            }

            @Override
            public void onDeleteMenu(MenuItem menuItem) {
                showDeleteDialog(menuItem);
            }
        });
        recyclerView.setAdapter(menuAdapter);

        // Add menu button
        btnAddMenu = findViewById(R.id.btnAddMenu);
        btnAddMenu.setOnClickListener(v -> showAddMenuDialog());

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation2);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_order_penjual) {
                startActivity(new Intent(MenuPenjualActivity.this, ActiveOrderActivity.class));
                return true;
            } else if (itemId == R.id.nav_menu) {
                return true;
            } else if (itemId == R.id.nav_profile_penjual) {
                startActivity(new Intent(MenuPenjualActivity.this, ProfilePenjualActivity.class));
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_menu);
    }

    private void initializeActivityResultLaunchers() {
        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                handleImageSelection(bitmap);
                            } catch (FileNotFoundException e) {
                                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap bitmap = (Bitmap) extras.get("data");
                            if (bitmap != null) {
                                handleImageSelection(bitmap);
                            }
                        }
                    }
                }
        );

        // Permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        showImagePickerDialog();
                    } else {
                        Toast.makeText(this, "Permission required to access camera/storage", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleImageSelection(Bitmap bitmap) {
        if (bitmap != null) {
            Bitmap resizedBitmap = resizeBitmap(bitmap, 800, 600);

            currentImageBase64 = bitmapToBase64(resizedBitmap);

            if (currentImageView != null && currentUploadPlaceholder != null) {
                currentImageView.setImageBitmap(resizedBitmap);
                currentImageView.setVisibility(ImageView.VISIBLE);
                currentUploadPlaceholder.setVisibility(LinearLayout.GONE);
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap originalBitmap, int maxWidth, int maxHeight) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;
        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }

        return Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return null;
        }
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source")
                .setItems(new String[]{"Gallery", "Camera"}, (dialog, which) -> {
                    if (which == 0) {
                        // Gallery
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(galleryIntent);
                    } else {
                        // Camera
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(cameraIntent);
                    }
                })
                .show();
    }

    private void requestImagePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            showImagePickerDialog();
        }
    }

    private void loadMenuItems() {
        mDatabase.child("menus").child(canteenId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                menuList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MenuItem menuItem = dataSnapshot.getValue(MenuItem.class);
                    if (menuItem != null) {
                        menuItem.setId(dataSnapshot.getKey());
                        menuList.add(menuItem);
                    }
                }
                menuAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MenuPenjualActivity.this, "Failed to load menu items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddMenuDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_menu);

        // Configure dialog window
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);

            // Set dialog size
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);

            window.setGravity(Gravity.CENTER);
        }

        // Initialize views
        TextInputEditText etMenuName = dialog.findViewById(R.id.et_menu_name);
        TextInputEditText etPrice = dialog.findViewById(R.id.et_price);
        TextInputEditText etStock = dialog.findViewById(R.id.et_stock);
        TextInputEditText etDescription = dialog.findViewById(R.id.et_description);
        LinearLayout layoutUploadImage = dialog.findViewById(R.id.layout_upload_image);
        currentImageView = dialog.findViewById(R.id.iv_menu_image);
        currentUploadPlaceholder = dialog.findViewById(R.id.layout_upload_placeholder);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save);
        ImageView btnClose = dialog.findViewById(R.id.btn_close);

        // Reset image state
        currentImageBase64 = "";
        if (currentImageView != null) {
            currentImageView.setVisibility(ImageView.GONE);
        }
        if (currentUploadPlaceholder != null) {
            currentUploadPlaceholder.setVisibility(LinearLayout.VISIBLE);
        }

        // Close button
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Upload image click
        layoutUploadImage.setOnClickListener(v -> requestImagePermission());

        // Save button
        btnSave.setOnClickListener(v -> {
            String menuName = etMenuName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String stockStr = etStock.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (menuName.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                int stock = Integer.parseInt(stockStr);

                if (price <= 0) {
                    Toast.makeText(this, "Price must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (stock < 0) {
                    Toast.makeText(this, "Stock cannot be negative", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create menu item
                Map<String, Object> menuData = new HashMap<>();
                menuData.put("name", menuName);
                menuData.put("price", price);
                menuData.put("stock", stock);
                menuData.put("description", description);
                menuData.put("imageBase64", currentImageBase64);
                menuData.put("available", true);

                // Save to Firebase
                String menuId = mDatabase.child("menus").child(canteenId).push().getKey();
                mDatabase.child("menus").child(canteenId).child(menuId).setValue(menuData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Menu added successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to add menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid price and stock numbers", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showEditMenuDialog(MenuItem menuItem) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_menu);

        // Configure dialog window
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);

            window.setGravity(Gravity.CENTER);
        }

        // Initialize views
        TextInputEditText etMenuName = dialog.findViewById(R.id.et_menu_name);
        TextInputEditText etPrice = dialog.findViewById(R.id.et_price);
        TextInputEditText etStock = dialog.findViewById(R.id.et_stock);
        TextInputEditText etDescription = dialog.findViewById(R.id.et_description);
        LinearLayout layoutUploadImage = dialog.findViewById(R.id.layout_upload_image);
        currentImageView = dialog.findViewById(R.id.iv_menu_image);
        currentUploadPlaceholder = dialog.findViewById(R.id.layout_upload_placeholder);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save);
        ImageView btnClose = dialog.findViewById(R.id.btn_close);

        // Fill with existing data
        etMenuName.setText(menuItem.getName());
        etPrice.setText(String.valueOf((int)menuItem.getPrice()));
        etStock.setText(String.valueOf(menuItem.getStock()));
        etDescription.setText(menuItem.getDescription());

        // Load existing image
        currentImageBase64 = menuItem.getImageBase64() != null ? menuItem.getImageBase64() : "";
        if (!currentImageBase64.isEmpty()) {
            Bitmap bitmap = base64ToBitmap(currentImageBase64);
            if (bitmap != null && currentImageView != null && currentUploadPlaceholder != null) {
                currentImageView.setImageBitmap(bitmap);
                currentImageView.setVisibility(ImageView.VISIBLE);
                currentUploadPlaceholder.setVisibility(LinearLayout.GONE);
            }
        } else {
            if (currentImageView != null && currentUploadPlaceholder != null) {
                currentImageView.setVisibility(ImageView.GONE);
                currentUploadPlaceholder.setVisibility(LinearLayout.VISIBLE);
            }
        }

        // Close button
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Upload image click
        layoutUploadImage.setOnClickListener(v -> requestImagePermission());

        // Save button
        btnSave.setOnClickListener(v -> {
            String menuName = etMenuName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String stockStr = etStock.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (menuName.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                int stock = Integer.parseInt(stockStr);

                if (price <= 0) {
                    Toast.makeText(this, "Price must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (stock < 0) {
                    Toast.makeText(this, "Stock cannot be negative", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update menu item
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", menuName);
                updates.put("price", price);
                updates.put("stock", stock);
                updates.put("description", description);
                updates.put("imageBase64", currentImageBase64);

                // Update in Firebase
                mDatabase.child("menus").child(canteenId).child(menuItem.getId()).updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Menu updated successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to update menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid price and stock numbers", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showDeleteDialog(MenuItem menuItem) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_menu);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Initialize buttons
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnDelete = dialog.findViewById(R.id.btnDelete);

        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Delete button
        btnDelete.setOnClickListener(v -> {
            // Delete from Firebase
            mDatabase.child("menus").child(canteenId).child(menuItem.getId()).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Menu deleted successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }
}