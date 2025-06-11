package com.example.fotnews;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Connect to the XML layout
        setContentView(R.layout.activity_splash_screen);

        // (Optional) Access views
        ImageView logo = findViewById(R.id.logoImage);

        // Wait and go to next activity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // close splash screen
        }, SPLASH_TIME_OUT);
    }
}
