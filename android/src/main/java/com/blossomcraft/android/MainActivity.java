package com.blossomcraft.android;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.blossomcraft.android.fragment.MessagesFragment;
import com.blossomcraft.android.fragment.MusicFragment;
import com.blossomcraft.android.fragment.ProfileFragment;
import com.blossomcraft.android.fragment.ShopFragment;
import com.blossomcraft.android.fragment.VideosFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Authenticated shell with a bottom navigation bar mirroring the mobile website:
 * Shop, Music, Videos, Messages, Profile. Each tab swaps a fragment.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment;
            if (id == R.id.tab_shop) {
                fragment = new ShopFragment();
            } else if (id == R.id.tab_music) {
                fragment = new MusicFragment();
            } else if (id == R.id.tab_videos) {
                fragment = new VideosFragment();
            } else if (id == R.id.tab_messages) {
                fragment = new MessagesFragment();
            } else if (id == R.id.tab_profile) {
                fragment = new ProfileFragment();
            } else {
                return false;
            }
            show(fragment);
            return true;
        });

        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.tab_shop);
        }
    }

    private void show(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }
}
