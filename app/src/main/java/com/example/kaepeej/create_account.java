package com.example.kaepeej;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class create_account extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);

        // Fix padding for system bars (status bar/navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Initialize UI Elements
        MaterialButton btnSignUp = findViewById(R.id.btnSignUp);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // 2. Setup "CREATE ACCOUNT" Button Logic
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the Toast message
                Toast.makeText(create_account.this, "Account Created!", Toast.LENGTH_SHORT).show();

                // Proceed to Landing Page
                // Ensure you have a Java class named LandingPageActivity
                Intent intent = new Intent(create_account.this, landing_page.class);
                startActivity(intent);

                // Finish this activity so the user can't go back to the registration form
                finish();
            }
        });

        // 3. Setup "Already a player? Log In" Logic
        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go back to MainActivity (Login Screen)
                Intent intent = new Intent(create_account.this, MainActivity.class);
                startActivity(intent);

                // Finish current activity
                finish();
            }
        });
    }
}