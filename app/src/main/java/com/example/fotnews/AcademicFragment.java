package com.example.fotnews;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AcademicFragment extends Fragment {

    private DatabaseReference databaseReference;
    private LinearLayout loadingLayout, contentLayout, errorLayout;
    private ProgressBar progressBar;
    private Button retryButton;

    private List<News> academicNewsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_academic, container, false);

        initializeViews(view);
        initializeFirebase();
        setupClickListeners();
        fetchAcademicNews();

        return view;
    }

    private void initializeViews(View view) {
        loadingLayout = view.findViewById(R.id.academicLoadingLayout);
        contentLayout = view.findViewById(R.id.academicContentLayout);
        errorLayout = view.findViewById(R.id.academicErrorLayout);
        progressBar = view.findViewById(R.id.academicProgressBar);
        retryButton = view.findViewById(R.id.academicRetryButton);
    }

    private void initializeFirebase() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://fotnews-3c6f8-default-rtdb.firebaseio.com/");
            this.databaseReference = database.getReference("news/academic");
            Log.d("AcademicFragment", "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e("AcademicFragment", "Firebase initialization failed: " + e.getMessage());
            showError("Firebase initialization failed");
        }
    }

    private void setupClickListeners() {
        if (retryButton != null) {
            retryButton.setOnClickListener(v -> fetchAcademicNews());
        }
    }

    private void fetchAcademicNews() {
        if (databaseReference == null) {
            Log.e("AcademicFragment", "DatabaseReference is null, reinitializing Firebase");
            initializeFirebase();
            if (databaseReference == null) {
                showError("Database connection failed");
                return;
            }
        }

        showLoading();
        academicNewsList.clear();
        clearDynamicCards();

        Log.d("AcademicFragment", "Fetching academic news from: " + databaseReference.toString());

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("AcademicFragment", "Data received: " + dataSnapshot.exists());
                Log.d("AcademicFragment", "Children count: " + dataSnapshot.getChildrenCount());

                if (!dataSnapshot.exists()) {
                    Log.w("AcademicFragment", "No academic news data found");
                    showError("No academic news available");
                    return;
                }

                for (DataSnapshot newsSnapshot : dataSnapshot.getChildren()) {
                    Log.d("AcademicFragment", "Processing news: " + newsSnapshot.getKey());

                    try {
                        News news = newsSnapshot.getValue(News.class);
                        if (news != null) {
                            academicNewsList.add(news);
                            Log.d("AcademicFragment", "Successfully added: " + news.getTitle());
                        } else {
                            Log.w("AcademicFragment", "Failed to parse news: " + newsSnapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e("AcademicFragment", "Error parsing news: " + e.getMessage());
                    }
                }

                if (academicNewsList.isEmpty()) {
                    Log.w("AcademicFragment", "No valid academic news found");
                    showError("No valid academic news found");
                    return;
                }

                // Sort by timestamp (newest first)
                Collections.sort(academicNewsList, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                Log.d("AcademicFragment", "Total academic news loaded: " + academicNewsList.size());
                validateImageUrls();
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AcademicFragment", "Database error: " + databaseError.getMessage());
                Log.e("AcademicFragment", "Error code: " + databaseError.getCode());

                if (databaseError.getCode() == DatabaseError.PERMISSION_DENIED) {
                    showError("Permission denied - check Firebase rules");
                } else {
                    showError("Database error: " + databaseError.getMessage());
                }
            }
        });
    }

    private void updateUI() {
        hideLoading();

        if (academicNewsList.isEmpty()) {
            Log.w("AcademicFragment", "Academic news list is empty");
            showError("No academic news to display");
            return;
        }

        createAcademicNewsCards();
        showContent();
    }

    private void createAcademicNewsCards() {
        if (getContext() == null) {
            Log.e("AcademicFragment", "Context is null, cannot create cards");
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < academicNewsList.size(); i++) {
            News news = academicNewsList.get(i);
            Log.d("AcademicFragment", "Creating card for: " + news.getTitle());

            try {
                // Inflate the news card template
                View cardView = inflater.inflate(R.layout.news_card_template, null);

                // Find views in the card
                TextView titleView = cardView.findViewById(R.id.newsTitle);
                TextView timeView = cardView.findViewById(R.id.newsTime);
                TextView descriptionView = cardView.findViewById(R.id.newsDescription);
                TextView categoryBadge = cardView.findViewById(R.id.newsCategoryBadge);
                ImageView imageView = cardView.findViewById(R.id.newsImage);
                TextView seeMoreView = cardView.findViewById(R.id.newsSeeMore);

                // Populate with data
                titleView.setText(news.getTitle());
                timeView.setText(getTimeAgo(news.getTimestamp()));
                categoryBadge.setText("ACADEMIC");
                categoryBadge.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));

                // Set up expandable text functionality
                setupExpandableText(descriptionView, seeMoreView, news.getDescription());

                // Enhanced image loading with Firebase URL handling
                loadNewsImage(imageView, news.getImage(), news.getTitle());

                // Add card to layout
                contentLayout.addView(cardView);

                Log.d("AcademicFragment", "Card created successfully for: " + news.getTitle());

            } catch (Exception e) {
                Log.e("AcademicFragment", "Error creating card for: " + news.getTitle() + " - " + e.getMessage());
            }
        }
    }

    private void loadNewsImage(ImageView imageView, String imageUrl, String newsTitle) {
        Log.d("AcademicFragment", "Loading image for: " + newsTitle);
        Log.d("AcademicFragment", "Image URL: " + imageUrl);

        if (getContext() == null) {
            Log.e("AcademicFragment", "Context is null, cannot load image");
            return;
        }

        // Check if image URL is valid
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            Log.w("AcademicFragment", "No image URL provided for: " + newsTitle);
            imageView.setImageResource(R.drawable.logo);
            return;
        }

        // Handle different URL types based on your Firebase data
        if (imageUrl.contains("google.com/url")) {
            // Extract actual image URL from Google redirect (like news1)
            loadGoogleRedirectImage(imageView, imageUrl, newsTitle);
        } else if (imageUrl.startsWith("https://firebasestorage.googleapis.com/")) {
            // Firebase Storage URL (like news2-news5, events, sports)
            loadFirebaseStorageImage(imageView, imageUrl, newsTitle);
        } else if (imageUrl.startsWith("gs://")) {
            // Firebase Storage gs:// URL
            loadFirebaseStorageReference(imageView, imageUrl, newsTitle);
        } else {
            // Direct URL
            loadDirectImage(imageView, imageUrl, newsTitle);
        }
    }

    private void loadGoogleRedirectImage(ImageView imageView, String googleUrl, String newsTitle) {
        Log.d("AcademicFragment", "Processing Google redirect URL for: " + newsTitle);

        try {
            // Extract the actual URL from Google redirect
            String actualUrl = extractActualUrl(googleUrl);
            if (actualUrl != null && !actualUrl.isEmpty()) {
                loadDirectImage(imageView, actualUrl, newsTitle);
            } else {
                Log.w("AcademicFragment", "Could not extract actual URL from Google redirect");
                imageView.setImageResource(R.drawable.logo);
            }
        } catch (Exception e) {
            Log.e("AcademicFragment", "Error processing Google URL: " + e.getMessage());
            imageView.setImageResource(R.drawable.logo);
        }
    }

    private String extractActualUrl(String googleUrl) {
        try {
            // Extract URL from Google redirect
            if (googleUrl.contains("&url=")) {
                String[] parts = googleUrl.split("&url=");
                if (parts.length > 1) {
                    String encodedUrl = parts[1].split("&")[0];
                    return URLDecoder.decode(encodedUrl, "UTF-8");
                }
            }
        } catch (Exception e) {
            Log.e("AcademicFragment", "Error extracting URL: " + e.getMessage());
        }
        return null;
    }

    private void loadFirebaseStorageImage(ImageView imageView, String storageUrl, String newsTitle) {
        Log.d("AcademicFragment", "Loading Firebase Storage image: " + storageUrl);

        // For Firebase Storage HTTPS URLs, load directly with Glide
        Glide.with(getContext())
                .load(storageUrl)
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo)
                .centerCrop()
                .timeout(15000) // Longer timeout for Firebase Storage
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e("AcademicFragment", "Failed to load Firebase Storage image for: " + newsTitle);
                        if (e != null) {
                            Log.e("AcademicFragment", "Error: " + e.getMessage());
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("AcademicFragment", "Successfully loaded Firebase Storage image for: " + newsTitle);
                        return false;
                    }
                })
                .into(imageView);
    }

    private void loadFirebaseStorageReference(ImageView imageView, String gsUrl, String newsTitle) {
        Log.d("AcademicFragment", "Loading Firebase Storage reference: " + gsUrl);

        try {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference imageRef = storage.getReferenceFromUrl(gsUrl);

            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d("AcademicFragment", "Firebase Storage download URL obtained: " + uri.toString());
                loadDirectImage(imageView, uri.toString(), newsTitle);
            }).addOnFailureListener(exception -> {
                Log.e("AcademicFragment", "Failed to get Firebase Storage download URL: " + exception.getMessage());
                imageView.setImageResource(R.drawable.logo);
            });
        } catch (Exception e) {
            Log.e("AcademicFragment", "Error processing Firebase Storage reference: " + e.getMessage());
            imageView.setImageResource(R.drawable.logo);
        }
    }

    private void loadDirectImage(ImageView imageView, String imageUrl, String newsTitle) {
        Glide.with(getContext())
                .load(imageUrl)
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo)
                .fallback(R.drawable.logo)
                .centerCrop()
                .timeout(10000)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e("AcademicFragment", "Failed to load direct image for: " + newsTitle);
                        if (e != null) {
                            Log.e("AcademicFragment", "Glide error: " + e.getMessage());
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("AcademicFragment", "Successfully loaded direct image for: " + newsTitle);
                        return false;
                    }
                })
                .into(imageView);
    }

    private void setupExpandableText(TextView descriptionView, TextView seeMoreView, String fullText) {
        final int MAX_LINES = 3;
        final int CHARACTER_LIMIT = 150;

        if (fullText == null || fullText.isEmpty()) {
            descriptionView.setText("No description available");
            seeMoreView.setVisibility(View.GONE);
            return;
        }

        // Store the full text and truncated text
        String truncatedText = fullText.length() > CHARACTER_LIMIT ?
                fullText.substring(0, CHARACTER_LIMIT) + "..." : fullText;

        // Initially show truncated text
        descriptionView.setText(truncatedText);
        descriptionView.setMaxLines(MAX_LINES);

        // Show/hide "See more" button based on text length
        if (fullText.length() > CHARACTER_LIMIT) {
            seeMoreView.setVisibility(View.VISIBLE);
            seeMoreView.setText("See more...");

            // Set up click listener for expand/collapse
            seeMoreView.setOnClickListener(v -> {
                if (descriptionView.getMaxLines() == MAX_LINES) {
                    // Expand text
                    descriptionView.setText(fullText);
                    descriptionView.setMaxLines(Integer.MAX_VALUE);
                    seeMoreView.setText("See less");
                    Log.d("AcademicFragment", "Expanded text for news");
                } else {
                    // Collapse text
                    descriptionView.setText(truncatedText);
                    descriptionView.setMaxLines(MAX_LINES);
                    seeMoreView.setText("See more...");
                    Log.d("AcademicFragment", "Collapsed text for news");
                }
            });
        } else {
            // Hide "See more" if text is short
            seeMoreView.setVisibility(View.GONE);
        }
    }

    private void validateImageUrls() {
        for (News news : academicNewsList) {
            String imageUrl = news.getImage();
            Log.d("AcademicFragment", "Validating image URL for " + news.getTitle() + ": " + imageUrl);

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                // Check if it's a valid URL format
                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    if (imageUrl.contains("google.com/url")) {
                        Log.d("AcademicFragment", "Google redirect URL: " + imageUrl);
                    } else if (imageUrl.contains("firebasestorage.googleapis.com")) {
                        Log.d("AcademicFragment", "Firebase Storage URL: " + imageUrl);
                    } else {
                        Log.d("AcademicFragment", "Direct HTTP URL: " + imageUrl);
                    }
                } else if (imageUrl.startsWith("gs://")) {
                    Log.d("AcademicFragment", "Firebase Storage gs:// URL: " + imageUrl);
                } else {
                    Log.w("AcademicFragment", "Unknown URL format: " + imageUrl);
                }
            } else {
                Log.w("AcademicFragment", "Empty or null image URL for: " + news.getTitle());
            }
        }
    }

    private void clearDynamicCards() {
        if (contentLayout != null) {
            contentLayout.removeAllViews();
            Log.d("AcademicFragment", "Cleared dynamic cards");
        }
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;

        if (months > 0) {
            return months + " month" + (months > 1 ? "s" : "") + " ago";
        } else if (weeks > 0) {
            return weeks + " week" + (weeks > 1 ? "s" : "") + " ago";
        } else if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " min" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    private void showLoading() {
        if (loadingLayout != null) loadingLayout.setVisibility(View.VISIBLE);
        if (contentLayout != null) contentLayout.setVisibility(View.GONE);
        if (errorLayout != null) errorLayout.setVisibility(View.GONE);
        Log.d("AcademicFragment", "Showing loading state");
    }

    private void showContent() {
        if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
        if (contentLayout != null) contentLayout.setVisibility(View.VISIBLE);
        if (errorLayout != null) errorLayout.setVisibility(View.GONE);
        Log.d("AcademicFragment", "Showing content");
    }

    private void showError(String message) {
        if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
        if (contentLayout != null) contentLayout.setVisibility(View.GONE);
        if (errorLayout != null) {
            errorLayout.setVisibility(View.VISIBLE);
            TextView errorText = errorLayout.findViewById(R.id.academicErrorText);
            if (errorText != null) {
                errorText.setText(message);
            }
        }
        Log.e("AcademicFragment", "Showing error: " + message);
    }

    private void hideLoading() {
        if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("AcademicFragment", "Fragment destroyed");
    }
}
