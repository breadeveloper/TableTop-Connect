package com.example.kaepeej;

import android.content.DialogInterface; // Added for Dialog
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog; // Added for Dialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

public class landing_page extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landing_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialCardView btnProfile = findViewById(R.id.btnProfile);
        MaterialCardView btnSearch = findViewById(R.id.btnSearchGame);
        MaterialCardView btnHost = findViewById(R.id.btnHostGame);

        btnProfile.setOnClickListener(v -> {
            showProfileMenu(btnProfile);
        });

        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(landing_page.this, map.class);
            startActivity(intent);
        });

        btnHost.setOnClickListener(v -> {
            Intent intent = new Intent(landing_page.this, host_game.class);
            startActivity(intent);
        });
    }

    private void showProfileMenu(MaterialCardView anchor) {
        PopupMenu popup = new PopupMenu(landing_page.this, anchor);
        popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.menuProfile) {
                    Intent intent = new Intent(landing_page.this, profile.class);
                    startActivity(intent);
                    return true;
                }
                else if (id == R.id.menuLogout) {
                    // Call the confirmation dialog instead of logging out immediately
                    showLogoutDialog();
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    // New method to handle Logout confirmation
    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Log Out");
        builder.setMessage("Are you sure you want to leave the table?");

        // If user clicks "Log Out"
        builder.setPositiveButton("Log Out", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(landing_page.this, "See you next game!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(landing_page.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        // If user clicks "Stay"
        builder.setNegativeButton("Stay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Just close the dialog
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}