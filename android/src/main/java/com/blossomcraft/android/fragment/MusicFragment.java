package com.blossomcraft.android.fragment;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blossomcraft.android.Async;
import com.blossomcraft.android.BlossomApp;
import com.blossomcraft.android.R;
import com.blossomcraft.android.adapter.TrackAdapter;
import com.blossomcraft.core.ApiConfig;
import com.blossomcraft.core.BlossomCraft;
import com.blossomcraft.core.model.Track;
import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/** Music tab: All / Top / Favorites / My tabs with a simple streaming player. */
public class MusicFragment extends Fragment implements TrackAdapter.Listener {

    private BlossomCraft bc;
    private TrackAdapter adapter;
    private TextView nowPlaying;
    private MediaPlayer player;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music, container, false);
        bc = BlossomApp.core(requireContext());

        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TrackAdapter(this);
        list.setAdapter(adapter);
        nowPlaying = view.findViewById(R.id.now_playing);

        TabLayout tabs = view.findViewById(R.id.tabs);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadForTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        loadForTab(0);
        return view;
    }

    private void loadForTab(int position) {
        Callable<List<Track>> loader;
        switch (position) {
            case 1:
                loader = () -> bc.music().topTracks(20);
                break;
            case 2:
                loader = () -> bc.music().favoriteTracks();
                break;
            case 3:
                loader = () -> bc.music().myTracks();
                break;
            default:
                loader = () -> bc.music().publicTracks();
        }
        Async.run(loader::call, tracks -> adapter.submit(tracks), err -> toast(Async.message(err)));
    }

    @Override
    public void onPlay(Track t) {
        if (t.audioUrl == null || t.audioUrl.isEmpty()) {
            toast("Нет аудио");
            return;
        }
        String url = t.audioUrl.startsWith("http") ? t.audioUrl : ApiConfig.getBaseUrl() + "/" + t.audioUrl;
        try {
            if (player != null) {
                player.release();
            }
            player = new MediaPlayer();
            player.setDataSource(url);
            player.setOnPreparedListener(MediaPlayer::start);
            player.prepareAsync();
            nowPlaying.setText("Играет: " + t.title + " — " + t.artist);
            Async.run(() -> {
                bc.music().recordPlay(t.id);
                return null;
            }, x -> {
            }, e -> {
            });
        } catch (IOException e) {
            toast("Ошибка воспроизведения");
        }
    }

    @Override
    public void onToggleLike(Track t) {
        boolean willLike = !t.liked;
        Async.run(() -> {
            if (willLike) {
                bc.music().like(t.id);
            } else {
                bc.music().unlike(t.id);
            }
            return null;
        }, x -> {
            t.liked = willLike;
            adapter.notifyDataSetChanged();
        }, err -> toast(Async.message(err)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
