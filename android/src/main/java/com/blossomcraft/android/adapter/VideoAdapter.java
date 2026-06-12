package com.blossomcraft.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blossomcraft.android.R;
import com.blossomcraft.core.model.Video;

import java.util.ArrayList;
import java.util.List;

/** Renders the video feed; play opens the system/native player, like toggles via API. */
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VH> {

    public interface Listener {
        void onPlay(Video video);

        void onToggleLike(Video video);
    }

    private final List<Video> items = new ArrayList<>();
    private final Listener listener;

    public VideoAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Video> videos) {
        items.clear();
        items.addAll(videos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Video v = items.get(position);
        holder.title.setText(v.title);
        holder.author.setText("@" + (v.userName == null ? v.userId : v.userName)
                + "   \uD83D\uDC41 " + v.views + "   \u2665 " + v.likes);
        holder.description.setText(v.description == null ? "" : v.description);
        holder.like.setText(v.liked ? "\u2665" : "\u2661");
        holder.play.setOnClickListener(view -> listener.onPlay(v));
        holder.like.setOnClickListener(view -> listener.onToggleLike(v));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView author;
        final TextView description;
        final Button play;
        final Button like;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.video_title);
            author = itemView.findViewById(R.id.video_author);
            description = itemView.findViewById(R.id.video_description);
            play = itemView.findViewById(R.id.video_play);
            like = itemView.findViewById(R.id.video_like);
        }
    }
}
