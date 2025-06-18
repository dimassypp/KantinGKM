package com.example.ratemy.kantingkm.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ratemy.kantingkm.R;
import com.example.ratemy.kantingkm.models.MenuItem;

import java.util.List;

public class MenuPenjualAdapter extends RecyclerView.Adapter<MenuPenjualAdapter.MenuPenjualViewHolder> {

    public interface OnMenuActionListener {
        void onEditMenu(MenuItem menuItem);
        void onDeleteMenu(MenuItem menuItem);
    }

    private Context context;
    private List<MenuItem> menuList;
    private OnMenuActionListener listener;

    public MenuPenjualAdapter(Context context, List<MenuItem> menuList, OnMenuActionListener listener) {
        this.context = context;
        this.menuList = menuList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MenuPenjualViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_menu_penjual, parent, false);
        return new MenuPenjualViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuPenjualViewHolder holder, int position) {
        MenuItem menuItem = menuList.get(position);

        holder.tvFoodName.setText(menuItem.getName());
        holder.tvFoodDescription.setText(menuItem.getDescription());
        holder.tvFoodPrice.setText(String.format("Rp. %,.0f", menuItem.getPrice()));

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
            // No image available, show default
            holder.ivFoodImage.setImageResource(R.drawable.food);
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEditMenu(menuItem));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteMenu(menuItem));
    }

    @Override
    public int getItemCount() {
        return menuList.size();
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

    public static class MenuPenjualViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoodImage;
        TextView tvFoodName, tvFoodDescription, tvFoodPrice;
        TextView btnEdit, btnDelete;

        public MenuPenjualViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoodImage = itemView.findViewById(R.id.iv_food_image);
            tvFoodName = itemView.findViewById(R.id.tv_food_name);
            tvFoodDescription = itemView.findViewById(R.id.tv_food_description);
            tvFoodPrice = itemView.findViewById(R.id.tv_food_price);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}