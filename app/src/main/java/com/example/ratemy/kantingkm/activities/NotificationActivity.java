package com.example.ratemy.kantingkm.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.adapters.NotificationAdapter;
import com.example.ratemy.kantingkm.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private DatabaseReference mDatabase;
    private String currentUserId;
    private LinearLayout emptyStateLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> navigateToMainActivity());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews();
        loadNotifications();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(this, notificationList);
        recyclerView.setAdapter(notificationAdapter);
    }

    private void loadNotifications() {
        mDatabase.child("notifications").child(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        notificationList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Notification notification = dataSnapshot.getValue(Notification.class);
                            if (notification != null) {
                                notification.setId(dataSnapshot.getKey());
                                notificationList.add(notification);
                            }
                        }
                        // Sort by timestamp
                        Collections.sort(notificationList, (n1, n2) ->
                                Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                        notificationAdapter.notifyDataSetChanged();

                        if (notificationList.isEmpty()) {
                            recyclerView.setVisibility(android.view.View.GONE);
                            emptyStateLayout.setVisibility(android.view.View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(android.view.View.VISIBLE);
                            emptyStateLayout.setVisibility(android.view.View.GONE);
                        }

                        markAllAsRead();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(NotificationActivity.this,
                                "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void markAllAsRead() {
        for (Notification notification : notificationList) {
            if (!notification.isRead()) {
                mDatabase.child("notifications").child(currentUserId)
                        .child(notification.getId()).child("read").setValue(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            navigateToMainActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        navigateToMainActivity();
        super.onBackPressed();
    }

    private void navigateToMainActivity() {
        finish();
    }
}