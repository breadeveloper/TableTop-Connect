package com.example.tabletopconnect;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private ChipGroup chipGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 1. Find our interactive views
        chipGroup = findViewById(R.id.settingsChipGroup);
        TextInputEditText newGameInput = findViewById(R.id.newGameInput);
        Button addGameBtn = findViewById(R.id.addGameButton);
        Button saveBtn = findViewById(R.id.saveSettingsButton);

        // 2. Pre-load some existing mock games
        addGameChip("Catan");
        addGameChip("Scythe");
        addGameChip("Ticket to Ride");

        // 3. Logic for adding a NEW game
        addGameBtn.setOnClickListener(v -> {
            // Get the text from the box and remove extra spaces
            String gameName = newGameInput.getText().toString().trim();

            // If it's not empty, create the chip and clear the text box
            if (!gameName.isEmpty()) {
                addGameChip(gameName);
                newGameInput.setText("");
            }
        });

        // 4. Logic for the Save Button
        saveBtn.setOnClickListener(v -> {
            // TODO later: Save the Name, Email, and Chips to Firebase here

            // Close the settings screen and return to the Profile
            finish();
        });
    }

    // --- HELPER FUNCTION: BUILDS A DYNAMIC CHIP ---
    private void addGameChip(String gameName) {
        Chip chip = new Chip(this);
        chip.setText(gameName);

        // Make the "X" icon visible
        chip.setCloseIconVisible(true);

        // Apply our warm colors
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#DF9A57")));
        chip.setTextColor(Color.WHITE);

        // The magic line: When the X is clicked, instantly remove this chip from the group
        chip.setOnCloseIconClickListener(v -> chipGroup.removeView(chip));

        // Add the fully built chip to the screen
        chipGroup.addView(chip);
    }
}