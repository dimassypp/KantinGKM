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

        if (holder.ivFoodImage != null) {
            try {
                if (menuItem.getImageBase64() != null && !menuItem.getImageBase64().isEmpty()) {
                    Bitmap bitmap = base64ToBitmap(menuItem.getImageBase64());
                    if (bitmap != null) {
                        holder.ivFoodImage.setImageBitmap(bitmap);
                    } else {
                        holder.ivFoodImage.setImageResource(R.drawable.food);
                    }
                } else if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().isEmpty()) {
                    Glide.with(context).load(menuItem.getImageUrl()).into(holder.ivFoodImage);
                } else {
                    holder.ivFoodImage.setImageResource(R.drawable.food);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image for menu item: " + menuItem.getName(), e);
                holder.ivFoodImage.setImageResource(R.drawable.food);
            }
        } else {
            Log.w(TAG, "ivFoodImage is null in ViewHolder");
        }

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
                // Notify listener ketika quantity berkurang
                if (quantityChangeListener != null) {
                    quantityChangeListener.onQuantityChanged();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return menuList.size();
    }

    public List<OrderItem> getSelectedItems() {
        return selectedItems;
    }

    public boolean hasSelectedItems() {
        return selectedItems != null && !selectedItems.isEmpty();
    }

    private Bitmap base64ToBitmap(String base64String) {
        try {
            if (base64String == null || base64String.isEmpty()) {
                return null;
            }
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private OrderItem getSelectedItem(String menuId) {
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
    }

    private void increaseQuantity(String menuId) {
        for (OrderItem item : selectedItems) {
            if (item.getMenuId().equals(menuId)) {
                item.setQuantity(item.getQuantity() + 1);
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
                } else {
                    selectedItems.remove(i);
                }
                break;
            }
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

                if (ivFoodImage == null) Log.w("MenuAdapter", "ivFoodImage is null - check R.id.iv_food_image in item_menu.xml");
                if (tvTitle == null) Log.w("MenuAdapter", "tvTitle is null - check R.id.tvTitle in item_menu.xml");
                if (tvDescription == null) Log.w("MenuAdapter", "tvDescription is null - check R.id.tvDescription in item_menu.xml");
                if (tvPrice == null) Log.w("MenuAdapter", "tvPrice is null - check R.id.tvPrice in item_menu.xml");
                if (tvQuantity == null) Log.w("MenuAdapter", "tvQuantity is null - check R.id.tvQuantity in item_menu.xml");
                if (btnAdd == null) Log.w("MenuAdapter", "btnAdd is null - check R.id.btnAdd in item_menu.xml");
                if (btnPlus == null) Log.w("MenuAdapter", "btnPlus is null - check R.id.btnPlus in item_menu.xml");
                if (btnMinus == null) Log.w("MenuAdapter", "btnMinus is null - check R.id.btnMinus in item_menu.xml");
                if (quantityLayout == null) Log.w("MenuAdapter", "quantityLayout is null - check R.id.quantityLayout in item_menu.xml");

            } catch (Exception e) {
                Log.e("MenuAdapter", "Error initializing ViewHolder", e);
            }
        }
    }
}