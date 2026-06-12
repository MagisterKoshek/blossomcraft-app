package com.blossomcraft.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blossomcraft.android.R;
import com.blossomcraft.core.model.Product;

import java.util.ArrayList;
import java.util.List;

/** Renders shop products as cards (name, price, seller, description). */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    private final List<Product> items = new ArrayList<>();

    public void submit(List<Product> products) {
        items.clear();
        items.addAll(products);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Product p = items.get(position);
        holder.name.setText(p.name);
        holder.price.setText(p.price == null ? "" : p.price + " \u20BD");
        holder.seller.setText(p.sellerName == null ? "" : "\u041F\u0440\u043E\u0434\u0430\u0432\u0435\u0446: " + p.sellerName);
        holder.description.setText(p.description == null ? "" : p.description);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView price;
        final TextView seller;
        final TextView description;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.product_name);
            price = itemView.findViewById(R.id.product_price);
            seller = itemView.findViewById(R.id.product_seller);
            description = itemView.findViewById(R.id.product_description);
        }
    }
}
