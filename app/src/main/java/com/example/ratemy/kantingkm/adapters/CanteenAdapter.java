package com.example.ratemy.kantingkm.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.models.Canteen;

import java.util.List;

public class CanteenAdapter extends RecyclerView.Adapter<CanteenAdapter.CanteenViewHolder> {

    private static final String TAG = "CanteenAdapter";

    private Context context;
    private List<Canteen> canteenList;
    private OnCanteenClickListener onCanteenClickListener;

    public CanteenAdapter(Context context, List<Canteen> canteenList, OnCanteenClickListener listener) {
        this.context = context;
        this.canteenList = canteenList;
        this.onCanteenClickListener = listener;
    }

    public CanteenAdapter(Context context, List<Canteen> canteenList) {
        this.context = context;
        this.canteenList = canteenList;
    }

    @NonNull
    @Override
    public CanteenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_canteen, parent, false);
        return new CanteenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CanteenViewHolder holder, int position) {
        Canteen canteen = canteenList.get(position);

        holder.tvCanteenName.setText(canteen.getName());
        holder.tvCanteenCategory.setText(canteen.getCategory());

        String imageData = canteen.getImageUrl();

        if (imageData != null && !imageData.trim().isEmpty()) {
            Log.d(TAG, "Loading image for canteen: " + canteen.getName() + " (data length: " + imageData.length() + ")");

            if (imageData.startsWith("http://") || imageData.startsWith("https://")) {
                Log.d(TAG, "Loading URL image for: " + canteen.getName());
                Glide.with(context)
                        .load(imageData)
                        .placeholder(R.drawable.kantinimg)
                        .error(R.drawable.kantinimg)
                        .into(holder.ivCanteenImage);
            }
            else if (imageData.startsWith("data:image/")) {
                Log.d(TAG, "Loading data URL image for: " + canteen.getName());
                loadBase64Image(holder.ivCanteenImage, imageData);
            }
            else if (isPotentialBase64(imageData)) {
                Log.d(TAG, "Loading pure base64 image for: " + canteen.getName());
                loadBase64Image(holder.ivCanteenImage, imageData);
            }
            else {
                Log.w(TAG, "Unknown image format for: " + canteen.getName() + ", first 50 chars: " +
                        imageData.substring(0, Math.min(50, imageData.length())));
                holder.ivCanteenImage.setImageResource(R.drawable.kantinimg);
            }
        } else {
            Log.d(TAG, "No image data for canteen: " + canteen.getName());
            holder.ivCanteenImage.setImageResource(R.drawable.kantinimg);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onCanteenClickListener != null) {
                onCanteenClickListener.onCanteenClick(canteen);
            }
        });
    }

    private boolean isPotentialBase64(String imageData) {
        if (imageData == null || imageData.trim().isEmpty()) {
            return false;
        }

        String trimmed = imageData.trim();

        if (trimmed.length() < 100) {
            return false;
        }

        String cleanData = trimmed.replaceAll("\\s+", "");

        boolean matchesPattern = cleanData.matches("^[A-Za-z0-9+/]*={0,2}$");

        boolean validLength = cleanData.length() % 4 == 0;

        Log.d(TAG, "Base64 check - Pattern: " + matchesPattern + ", Length: " + validLength +
                ", Data length: " + cleanData.length());

        return matchesPattern && validLength;
    }

    private void loadBase64Image(ImageView imageView, String base64String) {
        try {
            String base64Data = base64String.trim();

            if (base64Data.startsWith("data:image/")) {
                int commaIndex = base64Data.indexOf(',');
                if (commaIndex != -1) {
                    base64Data = base64Data.substring(commaIndex + 1);
                }
            }

            base64Data = base64Data.replaceAll("\\s+", "");

            Log.d(TAG, "Attempting to decode base64 data, length: " + base64Data.length());

            byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
            Log.d(TAG, "Decoded byte array length: " + decodedString.length);

            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            if (bitmap != null) {
                Log.d(TAG, "Successfully decoded base64 image - Bitmap size: " +
                        bitmap.getWidth() + "x" + bitmap.getHeight());
                imageView.setImageBitmap(bitmap);
            } else {
                Log.e(TAG, "Failed to decode base64 to bitmap - BitmapFactory returned null");
                imageView.setImageResource(R.drawable.kantinimg);
            }

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid base64 format: " + e.getMessage());
            imageView.setImageResource(R.drawable.kantinimg);
        } catch (Exception e) {
            Log.e(TAG, "Error loading base64 image: " + e.getMessage(), e);
            imageView.setImageResource(R.drawable.kantinimg);
        }
    }

    @Override
    public int getItemCount() {
        return canteenList.size();
    }

    public interface OnCanteenClickListener {
        void onCanteenClick(Canteen canteen);
    }

    public static class CanteenViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCanteenImage;
        TextView tvCanteenName, tvCanteenCategory;

        public CanteenViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCanteenImage = itemView.findViewById(R.id.imageView3);
            tvCanteenName = itemView.findViewById(R.id.tvCanteenName);
            tvCanteenCategory = itemView.findViewById(R.id.tvMenu);
        }
    }
}