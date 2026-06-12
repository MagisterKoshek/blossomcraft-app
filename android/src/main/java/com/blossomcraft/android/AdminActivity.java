package com.blossomcraft.android;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.blossomcraft.core.BlossomCraft;
import com.blossomcraft.core.model.User;

import java.util.List;

/**
 * Admin panel mirroring the website: list every account and rename, change role,
 * or delete. Reachable from the Profile tab for accounts with {@code manage_users}.
 * The backend re-checks permissions, so this screen is a convenience, not a gate.
 * Built programmatically to keep the admin surface self-contained.
 */
public class AdminActivity extends AppCompatActivity {

    private interface TextCallback {
        void onText(String value);
    }

    private BlossomCraft bc;
    private LinearLayout container;
    private int pad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bc = BlossomApp.core(this);
        setTitle("Админ-панель");
        pad = (int) (16 * getResources().getDisplayMetrics().density);

        ScrollView scroll = new ScrollView(this);
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad, pad, pad);
        scroll.addView(container);
        setContentView(scroll);

        load();
    }

    private void load() {
        message("Загрузка...");
        Async.run(() -> bc.admin().allUsers(), this::render,
                err -> message("Ошибка: " + Async.message(err)));
    }

    private void render(List<User> users) {
        container.removeAllViews();
        if (users == null || users.isEmpty()) {
            message("Пользователей нет");
            return;
        }
        for (User u : users) {
            container.addView(row(u));
        }
    }

    private LinearLayout row(User u) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad, pad, pad);

        TextView name = new TextView(this);
        name.setText((u.roleEmoji != null ? u.roleEmoji + " " : "") + u.displayName());
        name.setTextSize(16);

        TextView meta = new TextView(this);
        meta.setText((u.email == null ? "" : u.email) + "  •  "
                + (u.roleDisplay != null ? u.roleDisplay : u.roleId));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button rename = new Button(this);
        rename.setText("Имя");
        rename.setOnClickListener(v -> prompt("Новое имя", u.name, value ->
                Async.run(() -> {
                    bc.admin().setUsername(u.id, value);
                    return null;
                }, x -> load(), err -> toast(Async.message(err)))));

        Button role = new Button(this);
        role.setText("Роль");
        role.setOnClickListener(v -> prompt("ID роли", u.roleId, value ->
                Async.run(() -> {
                    bc.admin().setRole(u.id, value);
                    return null;
                }, x -> load(), err -> toast(Async.message(err)))));

        Button delete = new Button(this);
        delete.setText("Удалить");
        delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Удалить пользователя?")
                .setMessage(u.displayName())
                .setPositiveButton("Удалить", (d, w) -> Async.run(() -> {
                    bc.admin().deleteUser(u.id);
                    return null;
                }, x -> load(), err -> toast(Async.message(err))))
                .setNegativeButton("Отмена", null)
                .show());

        actions.addView(rename);
        actions.addView(role);
        actions.addView(delete);

        box.addView(name);
        box.addView(meta);
        box.addView(actions);
        return box;
    }

    private void prompt(String title, String initial, TextCallback callback) {
        final EditText field = new EditText(this);
        field.setText(initial == null ? "" : initial);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(field)
                .setPositiveButton("OK", (d, w) -> {
                    String value = field.getText().toString().trim();
                    if (!value.isEmpty()) {
                        callback.onText(value);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void message(String text) {
        container.removeAllViews();
        TextView t = new TextView(this);
        t.setText(text);
        container.addView(t);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
