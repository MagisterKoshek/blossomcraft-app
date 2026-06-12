package com.blossomcraft.android.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blossomcraft.android.R;
import com.blossomcraft.core.model.GroupMessage;
import com.blossomcraft.core.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat bubbles for both DM and group threads. Bubbles authored by the current
 * user align right; others align left and show the author name.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private static class Row {
        final String text;
        final String author;
        final boolean mine;

        Row(String text, String author, boolean mine) {
            this.text = text;
            this.author = author;
            this.mine = mine;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final String me;

    public ChatAdapter(String me) {
        this.me = me == null ? "" : me;
    }

    public void setDirectMessages(List<Message> messages) {
        rows.clear();
        for (Message m : messages) {
            boolean mine = me.equals(m.fromId);
            rows.add(new Row(m.text, mine ? null : m.fromName, mine));
        }
        notifyDataSetChanged();
    }

    public void setGroupMessages(List<GroupMessage> messages) {
        rows.clear();
        for (GroupMessage m : messages) {
            boolean mine = me.equals(m.userId);
            rows.add(new Row(m.text, mine ? null : m.userName, mine));
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Row row = rows.get(position);
        holder.bubble.setText((row.author != null ? row.author + "\n" : "") + (row.text == null ? "" : row.text));
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) holder.bubble.getLayoutParams();
        lp.gravity = row.mine ? Gravity.END : Gravity.START;
        holder.bubble.setLayoutParams(lp);
        holder.bubble.setBackgroundResource(row.mine ? R.drawable.bubble_out : R.drawable.bubble_in);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView bubble;

        VH(@NonNull View itemView) {
            super(itemView);
            bubble = itemView.findViewById(R.id.bubble_text);
        }
    }
}
