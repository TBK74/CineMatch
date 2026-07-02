package com.example.cinematch.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.cinematch.R;
import com.example.cinematch.ui.home.HomeFragment;
import com.example.cinematch.ui.profile.ProfileFragment;
import com.example.cinematch.ui.search.SearchFragment;
import com.example.cinematch.ui.watchlist.WatchlistFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// Activity chính, chứa 4 Fragment qua BottomNavigationView (Home/Search/Watchlist/Profile).
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            switchFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_search) {
                switchFragment(new SearchFragment());
                return true;
            } else if (id == R.id.nav_watchlist) {
                switchFragment(new WatchlistFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                switchFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    private void switchFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }
}
