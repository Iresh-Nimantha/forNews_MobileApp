package com.example.fotnews;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private Fragment currentFragment;
    private TextView headerTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        bottomNav = findViewById(R.id.bottom_navigation);
        ImageView headerLogo = findViewById(R.id.HeaderLogo);
        headerTitle = findViewById(R.id.HeaderTitle);

        // Setup bottom navigation
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Set header logo click listener
        headerLogo.setOnClickListener(v -> {
            Intent profileIntent = new Intent(MainActivity.this, UserProfileActivity.class);
            startActivity(profileIntent);
        });

        // Load default fragment and set initial title
        if (savedInstanceState == null) {
            loadFragment(new AcademicFragment());
            bottomNav.setSelectedItemId(R.id.academic);
            headerTitle.setText("Academic News");
        }
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.academic) {
                    selectedFragment = new AcademicFragment();
                    headerTitle.setText("Academic News");
                } else if (itemId == R.id.sport) {
                    selectedFragment = new SportsFragment();
                    headerTitle.setText("Sports News");
                } else if (itemId == R.id.event) {
                    selectedFragment = new EventsFragment();
                    headerTitle.setText("Events");
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
        transaction.replace(R.id.content_scroll, fragment);
        transaction.commit();
        currentFragment = fragment;
    }

    }