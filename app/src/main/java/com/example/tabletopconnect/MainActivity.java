package com.example.tabletopconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 1. Convert our 40dp XML measurement into raw pixels that Java understands
            int horizontalPadding = (int) android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()
            );

            // 2. Apply our custom horizontal padding + the system's vertical padding
            v.setPadding(horizontalPadding, systemBars.top, horizontalPadding, systemBars.bottom);

            return insets;
        });

        // Find the "Create an account" text view
        TextView createAccountText = findViewById(R.id.createAccountTextView);

        // Set a click listener using a modern lambda expression
        createAccountText.setOnClickListener(v -> {
            // Create an Intent to travel from MainActivity to RegisterActivity
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Find the Login button
        Button loginBtn = findViewById(R.id.loginButton);

        // Set the click listener
        loginBtn.setOnClickListener(v -> {
            // 1. Create the Intent to travel to HomeActivity
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);

            // 2. Start the new screen
            startActivity(intent);

            // 3. Close the Login screen so it isn't running in the background
            finish();
        });
    }
}