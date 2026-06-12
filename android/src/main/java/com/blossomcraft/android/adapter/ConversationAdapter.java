package com.blossomcraft.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blossomcraft.android.R;
import com.blossomcraft.core.model.Group;
import com.blossomcraft.core.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified list of conversations: people (DMs) and groups/channels. Tapping a row
 * opens {@code ChatActivity}.
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.VH> {

    public interface Listener {
        void onOpenPerson(User user);

        void onOpenGroup(Group group);
    }

    private static class Entry {
        final User user;
        final Group group;

        Entry(User user, Group group) {
            this.user = user;
            this.group = group;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Listener listener;

    public ConversationAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Group> groups, List<User> people) {
        entries.clear();
        if (groups != null) {
            for (Group g : groups) {
                entries.add(new Entry(null, g));
            }
        }
        if (people != null) {
            for (User u : people) {
                entries.add(new Entry(u, null));
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Entry e = entries.get(position);
        if (e.group != null) {
            Group g = e.group;
            holder.title.setText((g.isChannel() ? "\uD83D\uDCE2 " : "\uD83D\uDC65 ") + g.name);
            holder.subtitle.setText(g.isChannel() ? "\u041A\u0430\u043D\u0430\u043B" : "\u0413\u0440\u0443\u043F\u043F\u0430");
            holder.itemView.setOnClickListener(v -> listener.onOpenGroup(g));
        } else {
            User u = e.user;
            holder.title.setText((u.roleEmoji != null ? u.roleEmoji + " " : "") + u.displayName());
            holder.subtitle.setText(u.roleDisplay != null ? u.roleDisplay : "");
            holder.itemView.setOnClickListener(v -> listener.onOpenPerson(u));
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.conv_title);
            subtitle = itemView.findViewById(R.id.conv_subtitle);
        }
    }
}
