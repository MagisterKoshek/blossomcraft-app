package com.blossomcraft.android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.blossomcraft.android.Async;
import com.blossomcraft.android.BlossomApp;
import com.blossomcraft.android.R;
import com.blossomcraft.android.adapter.ProductAdapter;
import com.blossomcraft.core.BlossomCraft;

/** Shop tab: a pull-to-refresh list of products from the API. */
public class ShopFragment extends Fragment {

    private ProductAdapter adapter;
    private SwipeRefreshLayout refresh;
    private BlossomCraft bc;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        bc = BlossomApp.core(requireContext());

        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProductAdapter();
        list.setAdapter(adapter);

        refresh = view.findViewById(R.id.refresh);
        refresh.setOnRefreshListener(this::load);
        load();
        return view;
    }

    private void load() {
        refresh.setRefreshing(true);
        Async.run(() -> bc.shop().listProducts(0), products -> {
            refresh.setRefreshing(false);
            adapter.submit(products);
        }, err -> {
            refresh.setRefreshing(false);
            Toast.makeText(requireContext(), Async.message(err), Toast.LENGTH_LONG).show();
        });
    }
}
