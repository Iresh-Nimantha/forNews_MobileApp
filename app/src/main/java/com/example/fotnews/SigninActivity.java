package com.example.fotnews;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class SigninActivity extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button signInButton;
    private TextView logInLink;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin); // Use your XML file name

        usernameEditText = findViewById(R.id.username);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirm_password);
        signInButton = findViewById(R.id.signin_button);
        logInLink = findViewById(R.id.Log_in);
        progressBar = new ProgressBar(this);
        mAuth = FirebaseAuth.getInstance();

        TextView logInLink = findViewById(R.id.Log_in);
        logInLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the LoginActivity (replace with your actual login activity class)
                Intent intent = new Intent(SigninActivity.this, LoginActivity.class);
                startActivity(intent);
                // Optionally, finish the current activity so user can't go back to registration
                finish();
            }
        });



        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        logInLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go to the Login Activity
                Intent intent = new Intent(SigninActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Enter user name");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Enter email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Enter password");
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Password should be at least 6 characters");
            return;
        }

        // Optionally show a progress bar here
        signInButton.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(SigninActivity.this, new OnCompleteListener<com.google.firebase.auth.AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.auth.AuthResult> task) {
                        signInButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            // Registration success
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(SigninActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            // Optionally, update user profile with username
                            // Redirect to main activity or login
                            Intent intent = new Intent(SigninActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Registration failed
                            String errorMessage = task.getException().getMessage();
                            Log.e("FirebaseAuth", "Registration failed: " + errorMessage); // 🔴 Console log
                            Toast.makeText(SigninActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }

                    }
                });
    }
}
