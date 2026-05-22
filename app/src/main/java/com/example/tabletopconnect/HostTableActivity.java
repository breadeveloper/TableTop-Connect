package com.example.tabletopconnect;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class HostTableActivity extends AppCompatActivity {

    private int playerCount = 4;
    private boolean isPinLocked = false;

    // UI Elements for the Pin Toggle
    private Button btnTogglePin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_table);

        // --- BACK BUTTON ---
        ImageView backBtn = findViewById(R.id.backArrow);
        backBtn.setOnClickListener(v -> finish());

        // --- SLIDER LOGIC (Min 2, Max 20) ---
        com.google.android.material.slider.Slider playerSlider = findViewById(R.id.playerSlider);
        TextView txtCount = findViewById(R.id.sliderValueText);

        playerSlider.addOnChangeListener((slider, value, fromUser) -> {
            playerCount = (int) value; // Convert float to whole integer
            txtCount.setText(playerCount + " Players");
        });

        // --- MAP & VENUE LOGIC ---
        btnTogglePin = findViewById(R.id.btnTogglePin);
        Button btnCurrentLocation = findViewById(R.id.btnCurrentLocation);

        // Toggle Button Click
        btnTogglePin.setOnClickListener(v -> {
            isPinLocked = !isPinLocked; // Flip state
            updatePinUI();
        });

        // Current Location Click
        btnCurrentLocation.setOnClickListener(v -> {
            Toast.makeText(this, "Fetching Current Location...", Toast.LENGTH_SHORT).show();
            // User requested new location, so we AUTOMATICALLY unlock the pin for them
            isPinLocked = false;
            updatePinUI();
        });

        // --- LAUNCH SQUAD BUTTON ---
        Button launchBtn = findViewById(R.id.launchSquadButton);
        launchBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Table Launched Successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // Helper method to keep the Pin Toggle UI consistent
    private void updatePinUI() {
        if (isPinLocked) {
            btnTogglePin.setText("PIN LOCKED");
            btnTogglePin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#5E5B52"))); // Solid Olive
            btnTogglePin.setTextColor(Color.WHITE);
        } else {
            btnTogglePin.setText("EDIT PIN");
            btnTogglePin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E1BEE7"))); // Soft Purple
            btnTogglePin.setTextColor(Color.parseColor("#4A148C"));
        }
    }
}