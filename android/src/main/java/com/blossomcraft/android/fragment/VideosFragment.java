package com.blossomcraft.android.fragment;

import android.content.Intent;
import android.net.Uri;
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
import com.blossomcraft.android.adapter.VideoAdapter;
import com.blossomcraft.core.ApiConfig;
import com.blossomcraft.core.BlossomCraft;
import com.blossomcraft.core.model.Video;

/** Videos tab: a refreshable feed; playback opens the device video player. */
public class VideosFragment extends Fragment implements VideoAdapter.Listener {

    private BlossomCraft bc;
    private VideoAdapter adapter;
    private SwipeRefreshLayout refresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        bc = BlossomApp.core(requireContext());

        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VideoAdapter(this);
        list.setAdapter(adapter);

        refresh = view.findViewById(R.id.refresh);
        refresh.setOnRefreshListener(this::load);
        load();
        return view;
    }

    private void load() {
        refresh.setRefreshing(true);
        Async.run(() -> bc.videos().feed(), videos -> {
            refresh.setRefreshing(false);
            adapter.submit(videos);
        }, err -> {
            refresh.setRefreshing(false);
            Toast.makeText(requireContext(), Async.message(err), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onPlay(Video v) {
        if (v.videoUrl == null || v.videoUrl.isEmpty()) {
            Toast.makeText(requireContext(), "Нет видео", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = v.videoUrl.startsWith("http") ? v.videoUrl : ApiConfig.getBaseUrl() + "/" + v.videoUrl;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setDataAndType(Uri.parse(url), "video/*");
        startActivity(intent);
    }

    @Override
    public void onToggleLike(Video v) {
        Async.run(() -> bc.videos().toggleLike(v.id), liked -> {
            v.liked = liked;
            adapter.notifyDataSetChanged();
        }, err -> Toast.makeText(requireContext(), Async.message(err), Toast.LENGTH_SHORT).show());
    }
}
