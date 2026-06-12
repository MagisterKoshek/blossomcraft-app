package com.blossomcraft.android;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blossomcraft.android.adapter.ChatAdapter;
import com.blossomcraft.core.BlossomCraft;
import com.blossomcraft.core.model.GroupMember;
import com.blossomcraft.core.model.GroupMessage;
import com.blossomcraft.core.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * A single conversation — either a direct message thread or a group/channel,
 * selected via intent extras. Channels disable the composer for non-admins.
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_KIND = "kind";       // "dm" | "group"
    public static final String EXTRA_ID = "id";           // peer id or group id
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CAN_POST = "canPost"; // for channels
    public static final String EXTRA_ROLE = "role";        // owner | admin | member

    private static final int MENU_MANAGE = 1001;

    private BlossomCraft bc;
    private ChatAdapter adapter;
    private EditText input;
    private String kind;
    private String id;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        bc = BlossomApp.core(this);

        kind = getIntent().getStringExtra(EXTRA_KIND);
        id = getIntent().getStringExtra(EXTRA_ID);
        role = getIntent().getStringExtra(EXTRA_ROLE);
        setTitle(getIntent().getStringExtra(EXTRA_TITLE));
        boolean canPost = getIntent().getBooleanExtra(EXTRA_CAN_POST, true);

        RecyclerView list = findViewById(R.id.chat_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        String me = bc.currentUser() != null ? bc.currentUser().id : "";
        adapter = new ChatAdapter(me);
        list.setAdapter(adapter);

        input = findViewById(R.id.chat_input);
        Button send = findViewById(R.id.chat_send);
        input.setEnabled(canPost);
        send.setEnabled(canPost);
        send.setOnClickListener(v -> send());

        load();
    }

    private void load() {
        if ("group".equals(kind)) {
            Async.run(() -> bc.groups().messages(id, 0),
                    msgs -> adapter.setGroupMessages(msgs),
                    err -> toast(Async.message(err)));
        } else {
            Async.run(() -> bc.messages().fetchMessages(0), all -> {
                List<Message> thread = new ArrayList<>();
                for (Message m : all) {
                    if (id.equals(m.fromId) || id.equals(m.toId)) {
                        thread.add(m);
                    }
                }
                adapter.setDirectMessages(thread);
            }, err -> toast(Async.message(err)));
        }
    }

    private void send() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        input.setText("");
        if ("group".equals(kind)) {
            Async.run(() -> bc.groups().sendMessage(id, text), x -> load(), err -> toast(Async.message(err)));
        } else {
            Async.run(() -> bc.messages().send(id, text), x -> load(), err -> toast(Async.message(err)));
        }
    }

    // ─── Group / channel management (owners & admins) ─────────────────────
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if ("group".equals(kind) && ("owner".equals(role) || "admin".equals(role))) {
            menu.add(Menu.NONE, MENU_MANAGE, Menu.NONE, "Управление")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_MANAGE) {
            openManage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openManage() {
        boolean isOwner = "owner".equals(role);
        final List<String> opts = new ArrayList<>();
        opts.add("Название и описание");
        opts.add("Участники");
        opts.add("Код приглашения");
        if (isOwner) {
            opts.add("Удалить");
        }
        new AlertDialog.Builder(this)
                .setTitle("Управление")
                .setItems(opts.toArray(new String[0]), (d, which) -> {
                    String choice = opts.get(which);
                    if (choice.equals("Название и описание")) {
                        editGroup();
                    } else if (choice.equals("Участники")) {
                        manageMembers(isOwner);
                    } else if (choice.equals("Код приглашения")) {
                        regenerateCode();
                    } else if (choice.equals("Удалить")) {
                        confirmDelete();
                    }
                })
                .show();
    }

    private void editGroup() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);

        EditText nameField = new EditText(this);
        nameField.setHint("Название");
        nameField.setText(getTitle() == null ? "" : getTitle().toString());
        EditText descField = new EditText(this);
        descField.setHint("Описание");
        box.addView(nameField);
        box.addView(descField);

        new AlertDialog.Builder(this)
                .setTitle("Название и описание")
                .setView(box)
                .setPositiveButton("Сохранить", (d, w) -> {
                    String name = nameField.getText().toString().trim();
                    String desc = descField.getText().toString().trim();
                    Async.run(() -> {
                        bc.groups().update(id, name, desc, null);
                        return null;
                    }, x -> {
                        setTitle(name);
                        toast("Сохранено");
                    }, err -> toast(Async.message(err)));
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void manageMembers(boolean isOwner) {
        Async.run(() -> bc.groups().members(id),
                members -> showMembers(members, isOwner),
                err -> toast(Async.message(err)));
    }

    private void showMembers(List<GroupMember> members, boolean isOwner) {
        if (members == null || members.isEmpty()) {
            toast("Нет участников");
            return;
        }
        final List<GroupMember> list = members;
        String[] labels = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            GroupMember m = list.get(i);
            String badge = "owner".equals(m.role) ? "👑 " : "admin".equals(m.role) ? "⭐ " : "";
            String r = m.role == null ? "member" : m.role;
            labels[i] = badge + (m.userName != null ? m.userName : m.userId) + " (" + r + ")";
        }
        new AlertDialog.Builder(this)
                .setTitle("Участники")
                .setItems(labels, (d, which) -> memberActions(list.get(which), isOwner))
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void memberActions(GroupMember m, boolean isOwner) {
        boolean memberIsOwner = "owner".equals(m.role);
        boolean memberIsAdmin = "admin".equals(m.role);
        if (memberIsOwner) {
            toast("Это владелец");
            return;
        }
        final List<String> actions = new ArrayList<>();
        if (isOwner) {
            actions.add(memberIsAdmin ? "Снять администратора" : "Сделать администратором");
        }
        if (isOwner || !memberIsAdmin) {
            actions.add("Удалить из группы");
        }
        if (actions.isEmpty()) {
            toast("Нет доступных действий");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(m.userName != null ? m.userName : m.userId)
                .setItems(actions.toArray(new String[0]), (d, which) -> {
                    String a = actions.get(which);
                    if (a.equals("Сделать администратором")) {
                        changeRole(m, "admin", isOwner);
                    } else if (a.equals("Снять администратора")) {
                        changeRole(m, "member", isOwner);
                    } else if (a.equals("Удалить из группы")) {
                        kickMember(m, isOwner);
                    }
                })
                .show();
    }

    private void changeRole(GroupMember m, String newRole, boolean isOwner) {
        Async.run(() -> {
            bc.groups().setRole(id, m.userId, newRole);
            return null;
        }, x -> {
            toast("Готово");
            manageMembers(isOwner);
        }, err -> toast(Async.message(err)));
    }

    private void kickMember(GroupMember m, boolean isOwner) {
        Async.run(() -> {
            bc.groups().kick(id, m.userId);
            return null;
        }, x -> {
            toast("Удалён");
            manageMembers(isOwner);
        }, err -> toast(Async.message(err)));
    }

    private void regenerateCode() {
        Async.run(() -> bc.groups().regenerateCode(id),
                code -> new AlertDialog.Builder(this)
                        .setTitle("Код приглашения")
                        .setMessage(code == null ? "—" : code)
                        .setPositiveButton("OK", null)
                        .show(),
                err -> toast(Async.message(err)));
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить безвозвратно?")
                .setMessage("Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (d, w) -> Async.run(() -> {
                    bc.groups().delete(id);
                    return null;
                }, x -> {
                    toast("Удалено");
                    finish();
                }, err -> toast(Async.message(err))))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
