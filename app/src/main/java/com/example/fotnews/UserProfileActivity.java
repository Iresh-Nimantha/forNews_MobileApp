package com.example.fotnews;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private ImageView profileImage;
    private EditText usernameEditText;
    private TextView emailTextView;
    private Button editSaveButton, logoutButton, developerButton;
    private FirebaseAuth mAuth;
    private String currentPhotoPath;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE);

        initializeViews();
        loadUserData();
        loadProfileImage();
        setupButtonListeners();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profile_image);
        emailTextView = findViewById(R.id.email);
        usernameEditText = findViewById(R.id.username);
        editSaveButton = findViewById(R.id.edit_button);
        logoutButton = findViewById(R.id.logout_button);
        developerButton = findViewById(R.id.developer_button);
    }

    private void setupButtonListeners() {
        ImageButton changePhotoButton = findViewById(R.id.change_photo_button);

        editSaveButton.setOnClickListener(v -> toggleEditSave());
        logoutButton.setOnClickListener(v -> showCustomLogoutDialog());
        changePhotoButton.setOnClickListener(v -> showImageSourceDialog());

        developerButton.setOnClickListener(v -> {
            Log.d("UserProfile", "Developer button clicked");
            redirectToDeveloperActivity();
        });
    }

    private void redirectToDeveloperActivity() {
        try {
            Intent intent = new Intent(UserProfileActivity.this, DeveloperActivity.class);
            startActivity(intent);
            Log.d("UserProfile", "Successfully redirected to Developer activity");
        } catch (Exception e) {
            Log.e("UserProfile", "Error redirecting to Developer activity: " + e.getMessage());
            showToast("Error opening Developer screen");
        }
    }

    private void showCustomLogoutDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_logout_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button okButton = dialogView.findViewById(R.id.dialog_ok_button);
        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);

        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void performLogout() {
        mAuth.signOut();
        sharedPreferences.edit().clear().apply();
        Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void toggleEditSave() {
        if (editSaveButton.getText().equals("Edit")) {
            enableEditing();
        } else {
            saveUserName();
        }
    }

    private void enableEditing() {
        usernameEditText.setEnabled(true);
        usernameEditText.setBackgroundResource(android.R.drawable.edit_text);
        usernameEditText.requestFocus();
        usernameEditText.setSelection(usernameEditText.getText().length());
        editSaveButton.setText("Save");
    }

    private void saveUserName() {
        String newName = usernameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(newName)) {
            showToast("Name cannot be empty");
            return;
        }

        sharedPreferences.edit().putString("user_name", newName).apply();
        usernameEditText.setEnabled(false);
        usernameEditText.setBackgroundResource(android.R.color.transparent);
        editSaveButton.setText("Edit");
        showToast("Name updated successfully");
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            emailTextView.setText(user.getEmail());
            String savedName = sharedPreferences.getString("user_name", "");
            usernameEditText.setText(!savedName.isEmpty() ?
                    savedName : getDefaultDisplayName(user));
        }
    }

    private String getDefaultDisplayName(FirebaseUser user) {
        return user.getDisplayName() != null ? user.getDisplayName() : "User";
    }

    private void showImageSourceDialog() {
        Intent[] intents = new Intent[2];
        intents[0] = createCameraIntent();
        intents[1] = createGalleryIntent();

        Intent chooserIntent = Intent.createChooser(intents[1], "Select Image Source");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents);
        startActivityForResult(chooserIntent, PICK_IMAGE_REQUEST);
    }

    private Intent createCameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.fotnews.fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            }
        }
        return intent;
    }

    private Intent createGalleryIntent() {
        return new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);
            currentPhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            showToast("Error creating image file");
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            handleImageResult(requestCode, data);
        }
    }

    private void handleImageResult(int requestCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST) {
            if (data != null && data.getData() != null) {
                handleGalleryImage(data.getData());
            } else {
                handleCameraImage();
            }
        }
    }

    private void handleGalleryImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            saveAndDisplayImage(bitmap);
        } catch (IOException e) {
            showToast("Error loading image");
        }
    }

    private void handleCameraImage() {
        if (currentPhotoPath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            if (bitmap != null) {
                saveAndDisplayImage(bitmap);
            } else {
                showToast("Error capturing image");
            }
        }
    }

    private void saveAndDisplayImage(Bitmap bitmap) {
        try (FileOutputStream fos = openFileOutput("profile.jpg", MODE_PRIVATE)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            profileImage.setImageBitmap(bitmap);
            showToast("Profile image updated");
        } catch (IOException e) {
            showToast("Error saving image");
        }
    }

    private void loadProfileImage() {
        File file = new File(getFilesDir(), "profile.jpg");
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                profileImage.setImageBitmap(bitmap);
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
