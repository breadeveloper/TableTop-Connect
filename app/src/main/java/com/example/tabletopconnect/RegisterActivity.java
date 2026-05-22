package com.example.tabletopconnect;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int horizontalPadding = (int) android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()
            );

            v.setPadding(horizontalPadding, systemBars.top, horizontalPadding, systemBars.bottom);
            return insets;
        });

        // Find the "Login" text view at the bottom of the register screen
        TextView backToLoginText = findViewById(R.id.backToLoginTextView);

        // Set a click listener to close this screen and return to the previous one
        backToLoginText.setOnClickListener(v -> {
            // finish() destroys the current Activity and pops it off the stack
            finish();
        });
    }
}