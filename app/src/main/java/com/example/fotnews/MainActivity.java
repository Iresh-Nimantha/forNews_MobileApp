package com.example.fotnews;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize bottom navigation
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Load default fragment and set selected item
        if (savedInstanceState == null) {
            loadFragment(new AccademicFragment());
            bottomNav.setSelectedItemId(R.id.academic);
        }


    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.academic) {
                    selectedFragment = new AccademicFragment();
                } else if (itemId == R.id.sport) {
                    selectedFragment = new SportsFragment();
                } else if (itemId == R.id.event) {
                    selectedFragment = new EventsFragment();
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }
                return true;
            };

    private void loadFragment(Fragment fragment) {
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Disable transition animations
        transaction.setTransition(FragmentTransaction.TRANSIT_NONE);

        transaction.replace(R.id.content_scroll, fragment);
        transaction.commit();
        currentFragment = fragment;
    }


    @Override
    public void onBackPressed() {
        // Handle back press for bottom navigation
        if (bottomNav.getSelectedItemId() != R.id.academic) {
            // Go back to first tab (Academic)
            bottomNav.setSelectedItemId(R.id.academic);
        } else {
            // Exit app
            super.onBackPressed();
        }
    }
}