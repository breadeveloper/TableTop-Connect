package com.example.tabletopconnect;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReviewSessionActivity extends AppCompatActivity {

    private LinearLayout playerReviewContainer;
    private List<RatingBar> activeRatingBars = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_session);

        // Link UI
        playerReviewContainer = findViewById(R.id.playerReviewContainer);
        TextView tvReviewGameName = findViewById(R.id.tvReviewGameName);
        TextView tvDeadlineTimer = findViewById(R.id.tvDeadlineTimer);
        Button btnSubmitReviews = findViewById(R.id.btnSubmitReviews);

        // Get Game Name
        String gameName = getIntent().getStringExtra("GAME_NAME");
        if (gameName == null) gameName = "Marvels Rival Co-op";
        tvReviewGameName.setText(gameName);

        // --- CALCULATE EXACT 24-HOUR DEADLINE ---
        Calendar deadline = Calendar.getInstance();
        deadline.add(Calendar.HOUR_OF_DAY, 24);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.US);
        String exactDueTime = sdf.format(deadline.getTime());
        tvDeadlineTimer.setText("Due by: " + exactDueTime);

        // Load Players to Rate
        loadPlayersToRate();

        // Submit Logic
        btnSubmitReviews.setOnClickListener(v -> {
            boolean allRated = true;
            for (RatingBar rb : activeRatingBars) {
                if (rb.getRating() == 0) {
                    allRated = false;
                    break;
                }
            }

            if (!allRated) {
                Toast.makeText(this, "Please give a star rating to every player before submitting.", Toast.LENGTH_LONG).show();
            } else {
                // In production, this pushes the anonymous array of ratings to Firebase
                Toast.makeText(this, "Reviews submitted anonymously! Thank you.", Toast.LENGTH_LONG).show();
                finish(); // Returns them safely to the My Sessions tab
            }
        });
    }

    private void loadPlayersToRate() {
        // Exclude the current user from this list!
        addRatingRow("LocalPlayer_2");
        addRatingRow("LocalPlayer_3");
        addRatingRow("GameMaster_Daet");
    }

    private void addRatingRow(String playerName) {
        // Main Row Container
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(32, 32, 32, 48);
        row.setGravity(Gravity.CENTER);

        // Player Avatar (Transparent Background)
        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.happy); // Default icon
        avatar.setBackgroundResource(android.R.color.transparent);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(120, 120);
        avatarParams.setMargins(0, 0, 0, 16);
        avatar.setLayoutParams(avatarParams);
        row.addView(avatar);

        // Player Name
        TextView tvName = new TextView(this);
        tvName.setText(playerName);
        tvName.setTextColor(Color.parseColor("#2D2B30"));
        tvName.setTextSize(18);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setPadding(0, 0, 0, 16);
        row.addView(tvName);

        // Star Rating Bar
        RatingBar ratingBar = new RatingBar(this, null, android.R.attr.ratingBarStyleIndicator);
        ratingBar.setIsIndicator(false); // Make it clickable/draggable
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1.0f);
        ratingBar.setRating(0);

        LinearLayout.LayoutParams rbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ratingBar.setLayoutParams(rbParams);

        // Track the rating bar so we can validate it on Submit
        activeRatingBars.add(ratingBar);
        row.addView(ratingBar);

        playerReviewContainer.addView(row);

        // Subtle divider
        View divider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
        playerReviewContainer.addView(divider);
    }
}