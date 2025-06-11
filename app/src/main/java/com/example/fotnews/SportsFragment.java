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

public class SportsFragment extends Fragment {

    private DatabaseReference databaseReference;
    private LinearLayout loadingLayout, contentLayout, errorLayout;
    private ProgressBar progressBar;
    private Button retryButton;

    private List<News> sportsNewsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sports, container, false);

        initializeViews(view);
        initializeFirebase();
        setupClickListeners();
        fetchSportsNews();

        return view;
    }

    private void initializeViews(View view) {
        loadingLayout = view.findViewById(R.id.sportsLoadingLayout);
        contentLayout = view.findViewById(R.id.sportsContentLayout);
        errorLayout = view.findViewById(R.id.sportsErrorLayout);
        progressBar = view.findViewById(R.id.sportsProgressBar);
        retryButton = view.findViewById(R.id.sportsRetryButton);
    }

    private void initializeFirebase() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://fotnews-3c6f8-default-rtdb.firebaseio.com/");
            this.databaseReference = database.getReference("news/sports");
            Log.d("SportsFragment", "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e("SportsFragment", "Firebase initialization failed: " + e.getMessage());
            showError("Firebase initialization failed");
        }
    }

    private void setupClickListeners() {
        if (retryButton != null) {
            retryButton.setOnClickListener(v -> fetchSportsNews());
        }
    }

    private void fetchSportsNews() {
        if (databaseReference == null) {
            Log.e("SportsFragment", "DatabaseReference is null, reinitializing Firebase");
            initializeFirebase();
            if (databaseReference == null) {
                showError("Database connection failed");
                return;
            }
        }

        showLoading();
        sportsNewsList.clear();
        clearDynamicCards();

        Log.d("SportsFragment", "Fetching sports news from: " + databaseReference.toString());

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("SportsFragment", "Data received: " + dataSnapshot.exists());
                Log.d("SportsFragment", "Children count: " + dataSnapshot.getChildrenCount());

                if (!dataSnapshot.exists()) {
                    Log.w("SportsFragment", "No sports news data found");
                    showError("No sports news available");
                    return;
                }

                for (DataSnapshot newsSnapshot : dataSnapshot.getChildren()) {
                    Log.d("SportsFragment", "Processing news: " + newsSnapshot.getKey());

                    try {
                        News news = newsSnapshot.getValue(News.class);
                        if (news != null) {
                            sportsNewsList.add(news);
                            Log.d("SportsFragment", "Successfully added: " + news.getTitle());
                        } else {
                            Log.w("SportsFragment", "Failed to parse news: " + newsSnapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e("SportsFragment", "Error parsing news: " + e.getMessage());
                    }
                }

                if (sportsNewsList.isEmpty()) {
                    Log.w("SportsFragment", "No valid sports news found");
                    showError("No valid sports news found");
                    return;
                }

                // Sort by timestamp (newest first)
                Collections.sort(sportsNewsList, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                Log.d("SportsFragment", "Total sports news loaded: " + sportsNewsList.size());
                validateImageUrls();
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("SportsFragment", "Database error: " + databaseError.getMessage());
                Log.e("SportsFragment", "Error code: " + databaseError.getCode());

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

        if (sportsNewsList.isEmpty()) {
            Log.w("SportsFragment", "Sports news list is empty");
            showError("No sports news to display");
            return;
        }

        createSportsNewsCards();
        showContent();
    }

    private void createSportsNewsCards() {
        if (getContext() == null) {
            Log.e("SportsFragment", "Context is null, cannot create cards");
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < sportsNewsList.size(); i++) {
            News news = sportsNewsList.get(i);
            Log.d("SportsFragment", "Creating card for: " + news.getTitle());

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
                categoryBadge.setText("SPORTS");
                categoryBadge.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                // Set up expandable text functionality
                setupExpandableText(descriptionView, seeMoreView, news.getDescription());

                // Enhanced image loading with Firebase URL handling
                loadNewsImage(imageView, news.getImage(), news.getTitle());

                // Add card to layout
                contentLayout.addView(cardView);

                Log.d("SportsFragment", "Card created successfully for: " + news.getTitle());

            } catch (Exception e) {
                Log.e("SportsFragment", "Error creating card for: " + news.getTitle() + " - " + e.getMessage());
            }
        }
    }

    private void loadNewsImage(ImageView imageView, String imageUrl, String newsTitle) {
        Log.d("SportsFragment", "Loading image for: " + newsTitle);
        Log.d("SportsFragment", "Image URL: " + imageUrl);

        if (getContext() == null) {
            Log.e("SportsFragment", "Context is null, cannot load image");
            return;
        }

        // Check if image URL is valid
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            Log.w("SportsFragment", "No image URL provided for: " + newsTitle);
            imageView.setImageResource(R.drawable.logo);
            return;
        }

        // Handle different URL types based on your Firebase data
        if (imageUrl.contains("google.com/url")) {
            // Extract actual image URL from Google redirect
            loadGoogleRedirectImage(imageView, imageUrl, newsTitle);
        } else if (imageUrl.startsWith("https://firebasestorage.googleapis.com/")) {
            // Firebase Storage URL
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
        Log.d("SportsFragment", "Processing Google redirect URL for: " + newsTitle);

        try {
            // Extract the actual URL from Google redirect
            String actualUrl = extractActualUrl(googleUrl);
            if (actualUrl != null && !actualUrl.isEmpty()) {
                loadDirectImage(imageView, actualUrl, newsTitle);
            } else {
                Log.w("SportsFragment", "Could not extract actual URL from Google redirect");
                imageView.setImageResource(R.drawable.logo);
            }
        } catch (Exception e) {
            Log.e("SportsFragment", "Error processing Google URL: " + e.getMessage());
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
            Log.e("SportsFragment", "Error extracting URL: " + e.getMessage());
        }
        return null;
    }

    private void loadFirebaseStorageImage(ImageView imageView, String storageUrl, String newsTitle) {
        Log.d("SportsFragment", "Loading Firebase Storage image: " + storageUrl);

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
                        Log.e("SportsFragment", "Failed to load Firebase Storage image for: " + newsTitle);
                        if (e != null) {
                            Log.e("SportsFragment", "Error: " + e.getMessage());
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("SportsFragment", "Successfully loaded Firebase Storage image for: " + newsTitle);
                        return false;
                    }
                })
                .into(imageView);
    }

    private void loadFirebaseStorageReference(ImageView imageView, String gsUrl, String newsTitle) {
        Log.d("SportsFragment", "Loading Firebase Storage reference: " + gsUrl);

        try {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference imageRef = storage.getReferenceFromUrl(gsUrl);

            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d("SportsFragment", "Firebase Storage download URL obtained: " + uri.toString());
                loadDirectImage(imageView, uri.toString(), newsTitle);
            }).addOnFailureListener(exception -> {
                Log.e("SportsFragment", "Failed to get Firebase Storage download URL: " + exception.getMessage());
                imageView.setImageResource(R.drawable.logo);
            });
        } catch (Exception e) {
            Log.e("SportsFragment", "Error processing Firebase Storage reference: " + e.getMessage());
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
                        Log.e("SportsFragment", "Failed to load direct image for: " + newsTitle);
                        if (e != null) {
                            Log.e("SportsFragment", "Glide error: " + e.getMessage());
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("SportsFragment", "Successfully loaded direct image for: " + newsTitle);
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
                    Log.d("SportsFragment", "Expanded text for news");
                } else {
                    // Collapse text
                    descriptionView.setText(truncatedText);
                    descriptionView.setMaxLines(MAX_LINES);
                    seeMoreView.setText("See more...");
                    Log.d("SportsFragment", "Collapsed text for news");
                }
            });
        } else {
            // Hide "See more" if text is short
            seeMoreView.setVisibility(View.GONE);
        }
    }

    private void validateImageUrls() {
        for (News news : sportsNewsList) {
            String imageUrl = news.getImage();
            Log.d("SportsFragment", "Validating image URL for " + news.getTitle() + ": " + imageUrl);

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                // Check if it's a valid URL format
                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    if (imageUrl.contains("google.com/url")) {
                        Log.d("SportsFragment", "Google redirect URL: " + imageUrl);
                    } else if (imageUrl.contains("firebasestorage.googleapis.com")) {
                        Log.d("SportsFragment", "Firebase Storage URL: " + imageUrl);
                    } else {
                        Log.d("SportsFragment", "Direct HTTP URL: " + imageUrl);
                    }
                } else if (imageUrl.startsWith("gs://")) {
                    Log.d("SportsFragment", "Firebase Storage gs:// URL: " + imageUrl);
                } else {
                    Log.w("SportsFragment", "Unknown URL format: " + imageUrl);
                }
            } else {
                Log.w("SportsFragment", "Empty or null image URL for: " + news.getTitle());
            }
        }
    }

    private void clearDynamicCards() {
        if (contentLayout != null) {
            contentLayout.removeAllViews();
            Log.d("SportsFragment", "Cleared dynamic cards");
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
        Log.d("SportsFragment", "Showing loading state");
    }

    private void showContent() {
        if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
        if (contentLayout != null) contentLayout.setVisibility(View.VISIBLE);
        if (errorLayout != null) errorLayout.setVisibility(View.GONE);
        Log.d("SportsFragment", "Showing content");
    }

    private void showError(String message) {
        if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
        if (contentLayout != null) contentLayout.setVisibility(View.GONE);
        if (errorLayout != null) {
            errorLayout.setVisibility(View.VISIBLE);
            TextView errorText = errorLayout.findViewById(R.id.sportsErrorText);
            if (errorText != null) {
                errorText.setText(message);
            }
        }
        Log.e("SportsFragment", "Showing error: " + message);
    }

    private void hideLoading() {
        if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("SportsFragment", "Fragment destroyed");
    }
}
