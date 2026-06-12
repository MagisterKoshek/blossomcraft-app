package com.blossomcraft.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blossomcraft.android.R;
import com.blossomcraft.core.model.Track;

import java.util.ArrayList;
import java.util.List;

/** Renders music tracks with play and like actions. */
public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.VH> {

    public interface Listener {
        void onPlay(Track track);

        void onToggleLike(Track track);
    }

    private final List<Track> items = new ArrayList<>();
    private final Listener listener;

    public TrackAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Track> tracks) {
        items.clear();
        items.addAll(tracks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Track t = items.get(position);
        holder.title.setText(t.title);
        holder.artist.setText(t.artist);
        holder.meta.setText("\u25B6 " + t.plays + "   \u2665 " + t.likes);
        holder.like.setImageResource(t.liked
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);
        holder.play.setOnClickListener(v -> listener.onPlay(t));
        holder.like.setOnClickListener(v -> listener.onToggleLike(t));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView artist;
        final TextView meta;
        final ImageButton play;
        final ImageButton like;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.track_title);
            artist = itemView.findViewById(R.id.track_artist);
            meta = itemView.findViewById(R.id.track_meta);
            play = itemView.findViewById(R.id.track_play);
            like = itemView.findViewById(R.id.track_like);
        }
    }
}
