package com.example.ratemy.kantingkm.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ratemy.kantingkm.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class LoginPenjualActivity extends AppCompatActivity {

    private TextInputEditText etEmailPenjual, etPasswordPenjual;
    private Button btnLoginPenjual;
    private TextView tvSignInCustomer;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_penjual);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etEmailPenjual = findViewById(R.id.etEmailPenjual);
        etPasswordPenjual = findViewById(R.id.etPasswordPenjual);
        btnLoginPenjual = findViewById(R.id.btnLoginPenjual);
        tvSignInCustomer = findViewById(R.id.textViewSignInCustomer);

        btnLoginPenjual.setOnClickListener(v -> loginPenjual());

        tvSignInCustomer.setOnClickListener(v -> {
            startActivity(new Intent(LoginPenjualActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loginPenjual() {
        String email = etEmailPenjual.getText().toString().trim();
        String password = etPasswordPenjual.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmailPenjual.setError("Email cannot be empty");
            etEmailPenjual.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPasswordPenjual.setError("Password cannot be empty");
            etPasswordPenjual.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRoleAndSetupCanteen(user.getUid());
                        }
                    } else {
                        Toast.makeText(LoginPenjualActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRoleAndSetupCanteen(String userId) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    if (role != null && role.equals("penjual")) {
                        String existingCanteenId = snapshot.child("canteenId").getValue(String.class);

                        if (existingCanteenId != null && !existingCanteenId.isEmpty()) {
                            redirectToMenuActivity();
                        } else {
                            createCanteenForUser(userId, snapshot.child("name").getValue(String.class));
                        }
                    } else {
                        Toast.makeText(LoginPenjualActivity.this, "Please login as customer", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                } else {
                    Toast.makeText(LoginPenjualActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginPenjualActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createCanteenForUser(String userId, String userName) {
        String canteenId = mDatabase.child("canteens").push().getKey();

        if (canteenId == null) {
            Toast.makeText(this, "Failed to generate canteen ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create canteen data
        Map<String, Object> canteenData = new HashMap<>();
        canteenData.put("name", userName != null ? userName + "'s Canteen" : "My Canteen");
        canteenData.put("category", "Food & Beverage"); // Default category
        canteenData.put("ownerId", userId);
        canteenData.put("imageBase64", ""); // Empty image initially
        canteenData.put("description", "Welcome to our canteen!"); // Default description
        canteenData.put("createdAt", System.currentTimeMillis());

        // Save canteen to database
        mDatabase.child("canteens").child(canteenId).setValue(canteenData)
                .addOnSuccessListener(aVoid -> {
                    mDatabase.child("users").child(userId).child("canteenId").setValue(canteenId)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(LoginPenjualActivity.this, "Canteen created successfully!", Toast.LENGTH_SHORT).show();
                                redirectToMenuActivity();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(LoginPenjualActivity.this, "Failed to link canteen to user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                mDatabase.child("canteens").child(canteenId).removeValue();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginPenjualActivity.this, "Failed to create canteen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void redirectToMenuActivity() {
        startActivity(new Intent(LoginPenjualActivity.this, com.example.ratemy.kantingkm.activities.penjual.MenuPenjualActivity.class));
        finish();
    }
}