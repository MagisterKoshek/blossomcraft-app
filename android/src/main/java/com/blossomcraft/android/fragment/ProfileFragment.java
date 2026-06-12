package com.blossomcraft.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blossomcraft.android.AdminActivity;
import com.blossomcraft.android.Async;
import com.blossomcraft.android.BlossomApp;
import com.blossomcraft.android.AuthActivity;
import com.blossomcraft.android.R;
import com.blossomcraft.core.ApiConfig;
import com.blossomcraft.core.BlossomCraft;
import com.blossomcraft.core.model.User;
import com.blossomcraft.core.theme.Theme;

import java.util.LinkedHashMap;
import java.util.Map;

/** Profile tab: edit profile, pick theme, set API base, and log out. */
public class ProfileFragment extends Fragment {

    private BlossomCraft bc;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        bc = BlossomApp.core(requireContext());

        User user = bc.currentUser();
        TextView header = view.findViewById(R.id.profile_header);
        EditText name = view.findViewById(R.id.profile_name);
        EditText emoji = view.findViewById(R.id.profile_emoji);
        EditText color = view.findViewById(R.id.profile_color);
        EditText bio = view.findViewById(R.id.profile_bio);
        Spinner themeSpinner = view.findViewById(R.id.profile_theme);
        EditText apiBase = view.findViewById(R.id.profile_api);
        Button save = view.findViewById(R.id.profile_save);
        Button saveApi = view.findViewById(R.id.profile_save_api);
        Button logout = view.findViewById(R.id.profile_logout);

        if (user != null) {
            header.setText((user.roleEmoji != null ? user.roleEmoji + " " : "") + user.displayName());
            name.setText(user.name);
            emoji.setText(user.avatarEmoji);
            color.setText(user.avatarColor);
            bio.setText(user.bio);
        }

        Theme[] themes = Theme.values();
        String[] labels = new String[themes.length];
        for (int i = 0; i < themes.length; i++) {
            labels[i] = themes[i].label;
        }
        themeSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, labels));

        apiBase.setText(ApiConfig.getBaseUrl());

        save.setOnClickListener(v -> {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("name", name.getText().toString().trim());
            fields.put("avatarEmoji", emoji.getText().toString().trim());
            fields.put("avatarColor", color.getText().toString().trim());
            fields.put("bio", bio.getText().toString());
            Async.run(() -> {
                bc.profile().updateProfile(fields);
                return null;
            }, x -> toast("Профиль сохранён"), err -> toast(Async.message(err)));
        });

        saveApi.setOnClickListener(v -> {
            try {
                ApiConfig.setBaseUrl(apiBase.getText().toString().trim());
                toast("Адрес API обновлён");
            } catch (RuntimeException e) {
                toast(Async.message(e));
            }
        });

        logout.setOnClickListener(v -> {
            Async.run(() -> {
                bc.auth().logout();
                return null;
            }, x -> goToAuth(), err -> goToAuth());
        });

        Button admin = view.findViewById(R.id.profile_admin);
        if (bc.hasPermission("manage_users") || bc.hasPermission("*")) {
            admin.setVisibility(View.VISIBLE);
            admin.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), AdminActivity.class)));
        }

        return view;
    }

    private void goToAuth() {
        bc.clearSession();
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
