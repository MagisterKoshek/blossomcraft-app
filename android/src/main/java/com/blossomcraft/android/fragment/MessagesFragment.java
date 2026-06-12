package com.blossomcraft.android.fragment;

import android.content.Intent;
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
import com.blossomcraft.android.ChatActivity;
import com.blossomcraft.android.R;
import com.blossomcraft.android.adapter.ConversationAdapter;
import com.blossomcraft.core.BlossomCraft;
import com.blossomcraft.core.model.Group;
import com.blossomcraft.core.model.User;

import java.util.List;

/**
 * Messages tab: a single list combining groups/channels and people. Tapping a
 * row opens the conversation in {@link ChatActivity}.
 */
public class MessagesFragment extends Fragment implements ConversationAdapter.Listener {

    private BlossomCraft bc;
    private ConversationAdapter adapter;
    private SwipeRefreshLayout refresh;
    private List<Group> groups;
    private List<User> people;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        bc = BlossomApp.core(requireContext());

        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ConversationAdapter(this);
        list.setAdapter(adapter);

        refresh = view.findViewById(R.id.refresh);
        refresh.setOnRefreshListener(this::load);
        load();
        return view;
    }

    private void load() {
        refresh.setRefreshing(true);
        Async.run(() -> {
            groups = bc.groups().myGroups();
            people = bc.profile().searchUsers(null);
            return null;
        }, x -> {
            refresh.setRefreshing(false);
            adapter.submit(groups, people);
        }, err -> {
            refresh.setRefreshing(false);
            Toast.makeText(requireContext(), Async.message(err), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onOpenPerson(User user) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_KIND, "dm");
        intent.putExtra(ChatActivity.EXTRA_ID, user.id);
        intent.putExtra(ChatActivity.EXTRA_TITLE, user.displayName());
        startActivity(intent);
    }

    @Override
    public void onOpenGroup(Group group) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_KIND, "group");
        intent.putExtra(ChatActivity.EXTRA_ID, group.id);
        intent.putExtra(ChatActivity.EXTRA_TITLE, group.name);
        intent.putExtra(ChatActivity.EXTRA_CAN_POST, group.canPost(group.role));
        intent.putExtra(ChatActivity.EXTRA_ROLE, group.role);
        startActivity(intent);
    }
}
