package com.example.ratemy.kantingkm.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.models.MenuItem;
import com.example.ratemy.kantingkm.models.OrderItem;

import java.util.ArrayList;
import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

    private static final String TAG = "MenuAdapter";
    private Context context;
    private List<MenuItem> menuList;
    private List<OrderItem> selectedItems;
    private OnQuantityChangeListener quantityChangeListener;

    public interface OnQuantityChangeListener {
        void onQuantityChanged();
    }

    public MenuAdapter(Context context, List<MenuItem> menuList, OnQuantityChangeListener listener) {
        this.context = context;
        this.menuList = menuList;
        this.selectedItems = new ArrayList<>();
        this.quantityChangeListener = listener;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuItem menuItem = menuList.get(position);

        // Set text data
        if (holder.tvTitle != null) {
            holder.tvTitle.setText(menuItem.getName());
        }

        if (holder.tvDescription != null) {
            holder.tvDescription.setText(menuItem.getDescription());
        }

        if (holder.tvPrice != null) {
            holder.tvPrice.setText(String.format("Rp. %,.0f", menuItem.getPrice()));
        }

        loadMenuImage(holder, menuItem);

        OrderItem selectedItem = getSelectedItem(menuItem.getId());
        if (selectedItem != null) {
            if (holder.quantityLayout != null) {
                holder.quantityLayout.setVisibility(View.VISIBLE);
            }
            if (holder.btnAdd != null) {
                holder.btnAdd.setVisibility(View.GONE);
            }
            if (holder.tvQuantity != null) {
                holder.tvQuantity.setText(String.valueOf(selectedItem.getQuantity()));
            }
        } else {
            if (holder.quantityLayout != null) {
                holder.quantityLayout.setVisibility(View.GONE);
            }
            if (holder.btnAdd != null) {
                holder.btnAdd.setVisibility(View.VISIBLE);
            }
        }

        // Set button click listeners
        if (holder.btnAdd != null) {
            holder.btnAdd.setOnClickListener(v -> {
                addItemToOrder(menuItem);
                notifyItemChanged(position);
                if (quantityChangeListener != null) {
                    quantityChangeListener.onQuantityChanged();
                }
            });
        }

        if (holder.btnPlus != null) {
            holder.btnPlus.setOnClickListener(v -> {
                increaseQuantity(menuItem.getId());
                notifyItemChanged(position);
                if (quantityChangeListener != null) {
                    quantityChangeListener.onQuantityChanged();
                }
            });
        }

        if (holder.btnMinus != null) {
            holder.btnMinus.setOnClickListener(v -> {
                decreaseQuantity(menuItem.getId());
                notifyItemChanged(position);
                if (quantityChangeListener != null) {
                    quantityChangeListener.onQuantityChanged();
                }
            });
        }
    }

    private void loadMenuImage(MenuViewHolder holder, MenuItem menuItem) {
        if (holder.ivFoodImage == null) {
            Log.w(TAG, "ivFoodImage is null in ViewHolder");
            return;
        }

        try {
            Log.d(TAG, "Loading image for menu item: " + menuItem.getName());

            holder.ivFoodImage.setImageResource(R.drawable.food);

            if (menuItem.getImageBase64() != null && !menuItem.getImageBase64().trim().isEmpty()) {
                Log.d(TAG, "Attempting to load base64 image for: " + menuItem.getName());

                Bitmap bitmap = base64ToBitmap(menuItem.getImageBase64());
                if (bitmap != null) {
                    holder.ivFoodImage.setImageBitmap(bitmap);
                    Log.d(TAG, "Successfully loaded base64 image for: " + menuItem.getName());
                    return;
                } else {
                    Log.w(TAG, "Failed to decode base64 image for: " + menuItem.getName());
                }
            }

            if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().trim().isEmpty()) {
                Log.d(TAG, "Attempting to load URL image for: " + menuItem.getName() + " from: " + menuItem.getImageUrl());

                RequestOptions options = new RequestOptions()
                        .placeholder(R.drawable.food)
                        .error(R.drawable.food)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .timeout(10000);

                Glide.with(context)
                        .load(menuItem.getImageUrl())
                        .apply(options)
                        .into(holder.ivFoodImage);

                Log.d(TAG, "Glide load initiated for: " + menuItem.getName());
                return;
            }

            Log.d(TAG, "No image data available for: " + menuItem.getName() + ", using default image");

        } catch (Exception e) {
            Log.e(TAG, "Error loading image for menu item: " + menuItem.getName(), e);
            holder.ivFoodImage.setImageResource(R.drawable.food);
        }
    }

    private Bitmap base64ToBitmap(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return null;
        }

        try {
            String cleanBase64 = base64String;
            if (base64String.startsWith("data:image")) {
                int commaIndex = base64String.indexOf(",");
                if (commaIndex > 0) {
                    cleanBase64 = base64String.substring(commaIndex + 1);
                }
            }

            byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            if (bitmap != null) {
                // Resize to optimize memory usage
                bitmap = resizeBitmap(bitmap, 800, 600);
            }

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error converting base64 to bitmap", e);
            return null;
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

    @Override
    public int getItemCount() {
        return menuList != null ? menuList.size() : 0;
    }

    public List<OrderItem> getSelectedItems() {
        return selectedItems;
    }

    public boolean hasSelectedItems() {
        return selectedItems != null && !selectedItems.isEmpty();
    }

    private OrderItem getSelectedItem(String menuId) {
        if (selectedItems == null) return null;

        for (OrderItem item : selectedItems) {
            if (item.getMenuId().equals(menuId)) {
                return item;
            }
        }
        return null;
    }

    private void addItemToOrder(MenuItem menuItem) {
        OrderItem orderItem = new OrderItem();
        orderItem.setMenuId(menuItem.getId());
        orderItem.setName(menuItem.getName());
        orderItem.setPrice(menuItem.getPrice());
        orderItem.setQuantity(1);
        selectedItems.add(orderItem);

        Log.d(TAG, "Added item to order: " + menuItem.getName());
    }

    private void increaseQuantity(String menuId) {
        for (OrderItem item : selectedItems) {
            if (item.getMenuId().equals(menuId)) {
                item.setQuantity(item.getQuantity() + 1);
                Log.d(TAG, "Increased quantity for: " + item.getName() + " to " + item.getQuantity());
                break;
            }
        }
    }

    private void decreaseQuantity(String menuId) {
        for (int i = 0; i < selectedItems.size(); i++) {
            OrderItem item = selectedItems.get(i);
            if (item.getMenuId().equals(menuId)) {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                    Log.d(TAG, "Decreased quantity for: " + item.getName() + " to " + item.getQuantity());
                } else {
                    selectedItems.remove(i);
                    Log.d(TAG, "Removed item from order: " + item.getName());
                }
                break;
            }
        }
    }

    public void updateMenuList(List<MenuItem> newMenuList) {
        if (this.menuList == null) {
            this.menuList = new ArrayList<>();
        }

        this.menuList.clear();
        this.menuList.addAll(newMenuList);
        notifyDataSetChanged();
        Log.d(TAG, "Menu list updated with " + newMenuList.size() + " items");
    }

    public void clearSelectedItems() {
        if (selectedItems != null) {
            selectedItems.clear();
            notifyDataSetChanged();
            Log.d(TAG, "Selected items cleared");
        }
    }

    public static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoodImage;
        TextView tvTitle, tvDescription, tvPrice, tvQuantity;
        ImageButton btnAdd, btnPlus, btnMinus;
        LinearLayout quantityLayout;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);

            try {
                ivFoodImage = itemView.findViewById(R.id.iv_food_image);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                tvQuantity = itemView.findViewById(R.id.tvQuantity);
                btnAdd = itemView.findViewById(R.id.btnAdd);
                btnPlus = itemView.findViewById(R.id.btnPlus);
                btnMinus = itemView.findViewById(R.id.btnMinus);
                quantityLayout = itemView.findViewById(R.id.quantityLayout);

                if (ivFoodImage == null) Log.w(TAG, "ivFoodImage is null - check R.id.iv_food_image in item_menu.xml");
                if (tvTitle == null) Log.w(TAG, "tvTitle is null - check R.id.tvTitle in item_menu.xml");
                if (tvDescription == null) Log.w(TAG, "tvDescription is null - check R.id.tvDescription in item_menu.xml");
                if (tvPrice == null) Log.w(TAG, "tvPrice is null - check R.id.tvPrice in item_menu.xml");
                if (tvQuantity == null) Log.w(TAG, "tvQuantity is null - check R.id.tvQuantity in item_menu.xml");
                if (btnAdd == null) Log.w(TAG, "btnAdd is null - check R.id.btnAdd in item_menu.xml");
                if (btnPlus == null) Log.w(TAG, "btnPlus is null - check R.id.btnPlus in item_menu.xml");
                if (btnMinus == null) Log.w(TAG, "btnMinus is null - check R.id.btnMinus in item_menu.xml");
                if (quantityLayout == null) Log.w(TAG, "quantityLayout is null - check R.id.quantityLayout in item_menu.xml");

            } catch (Exception e) {
                Log.e(TAG, "Error initializing ViewHolder", e);
            }
        }
    }
}