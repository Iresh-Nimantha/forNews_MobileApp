package com.example.fotnews;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class DeveloperActivity extends AppCompatActivity {

    private TextView developerName, studentNumber, personalStatement, releaseVersion;
    private Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);

        initializeViews();
        setupClickListeners();
        loadDeveloperInfo();
    }

    private void initializeViews() {
        developerName = findViewById(R.id.developer_name);
        studentNumber = findViewById(R.id.student_number);
        personalStatement = findViewById(R.id.personal_statement);
        releaseVersion = findViewById(R.id.release_version);
        exitButton = findViewById(R.id.exit_button);
    }

    private void setupClickListeners() {
        exitButton.setOnClickListener(v -> {
            finish(); // Close this activity and return to previous screen
        });
    }

    private void loadDeveloperInfo() {
        // Set developer information
        developerName.setText("Jhon Dho");
        studentNumber.setText("2022T01558");
        personalStatement.setText("Passionate tech student eager to solve real-world problems through innovation and collaboration");
        releaseVersion.setText("2.v");
    }
}
