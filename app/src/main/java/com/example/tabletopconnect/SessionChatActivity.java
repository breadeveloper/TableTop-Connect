package com.example.tabletopconnect;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

public class SessionChatActivity extends AppCompatActivity {

    // --- MOCK STATE CONTROL ---
    // Change this to 'false' to test the standard player view!
    private boolean isHost = true;
    private boolean sessionStarted = false;

    // UI Elements
    private LinearLayout chatMessageContainer, activeRosterContainer, pendingListContainer, pendingRequestsSection;
    private ScrollView chatScrollView;
    private EditText etMessageInput;
    private View dimOverlay;
    private CardView cvTableMenu;
    private Button btnPrimaryAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_chat);

        // Header Data (Usually pulled from Intent)
        String gameName = getIntent().getStringExtra("GAME_NAME");
        if (gameName == null) gameName = "Marvels Rival Co-op";
        TextView tvChatGameName = findViewById(R.id.tvChatGameName);
        tvChatGameName.setText(gameName);

        // Link UI
        chatMessageContainer = findViewById(R.id.chatMessageContainer);
        chatScrollView = findViewById(R.id.chatScrollView);
        etMessageInput = findViewById(R.id.etMessageInput);
        dimOverlay = findViewById(R.id.dimOverlay);
        cvTableMenu = findViewById(R.id.cvTableMenu);
        activeRosterContainer = findViewById(R.id.activeRosterContainer);
        pendingListContainer = findViewById(R.id.pendingListContainer);
        pendingRequestsSection = findViewById(R.id.pendingRequestsSection);
        btnPrimaryAction = findViewById(R.id.btnPrimaryAction);

        // Setup Buttons
        findViewById(R.id.btnBackChat).setOnClickListener(v -> finish());

        findViewById(R.id.btnSendMessage).setOnClickListener(v -> {
            String msg = etMessageInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                addChatBubble(msg, true, "You");
                etMessageInput.setText("");
            }
        });

        // Toggle "All-in-One" Menu
        findViewById(R.id.btnTableInfo).setOnClickListener(v -> openTableMenu());
        dimOverlay.setOnClickListener(v -> closeTableMenu());

        setupRoleBasedMenu();
        loadMockChat();
    }

    // ==========================================
    // CHAT BUBBLE LOGIC
    // ==========================================
    private void loadMockChat() {
        addChatBubble("Hey everyone! Are we still good for 8 PM?", false, "LocalPlayer_2");
        addChatBubble("Yes! I'm bringing snacks. See you soon.", true, "You");
    }

    private void addChatBubble(String message, boolean isSelf, String senderName) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.gravity = isSelf ? Gravity.END : Gravity.START;
        rowParams.setMargins(0, 8, 0, 8);
        row.setLayoutParams(rowParams);

        // Sender Name
        if (!isSelf) {
            TextView tvName = new TextView(this);
            tvName.setText(senderName);
            tvName.setTextSize(12);
            tvName.setTextColor(Color.parseColor("#888888"));
            tvName.setPadding(8, 0, 0, 4);
            row.addView(tvName);
        }

        // Bubble
        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextSize(16);
        tvMsg.setPadding(32, 24, 32, 24);

        if (isSelf) {
            tvMsg.setBackgroundColor(Color.parseColor("#FC7A57")); // Coral
            tvMsg.setTextColor(Color.WHITE);
        } else {
            tvMsg.setBackgroundColor(Color.parseColor("#E0E0E0")); // Light Gray
            tvMsg.setTextColor(Color.parseColor("#2D2B30"));
        }

        // Simple corner rounding hack without a dedicated drawable XML
        tvMsg.setBackground(ContextCompat.getDrawable(this, android.R.drawable.dialog_holo_light_frame));
        if (isSelf) tvMsg.getBackground().setTint(Color.parseColor("#FC7A57"));
        else tvMsg.getBackground().setTint(Color.parseColor("#EAEAEA"));

        row.addView(tvMsg);
        chatMessageContainer.addView(row);

        // Auto-scroll to bottom
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }

    // ==========================================
    // TABLE MENU & ROSTER LOGIC
    // ==========================================
    private void openTableMenu() {
        dimOverlay.setVisibility(View.VISIBLE);
        cvTableMenu.setVisibility(View.VISIBLE);
    }

    private void closeTableMenu() {
        dimOverlay.setVisibility(View.GONE);
        cvTableMenu.setVisibility(View.GONE);
    }

    private void setupRoleBasedMenu() {
        activeRosterContainer.removeAllViews();
        pendingListContainer.removeAllViews();

        // Populate Mock Active Roster
        addPlayerToRoster("GameMaster_Daet", true, false);
        addPlayerToRoster("LocalPlayer_2", false, false);
        addPlayerToRoster("LocalPlayer_3", false, false);

        if (isHost) {
            // HOST CONTROLS
            pendingRequestsSection.setVisibility(View.VISIBLE);
            addPlayerToRoster("NewGuy_99", false, true); // Add a pending request

            updateHostPrimaryButton();

            btnPrimaryAction.setOnClickListener(v -> {
                if (!sessionStarted) {
                    sessionStarted = true;
                    updateHostPrimaryButton();
                    Toast.makeText(this, "Session Started! Map status updated.", Toast.LENGTH_SHORT).show();
                    closeTableMenu();
                } else {
                    // Trigger End Session & Review
                    Toast.makeText(this, "Session Ended. Launching Post-Match Review...", Toast.LENGTH_LONG).show();
                    finish(); // Drops them back to My Sessions to handle the review
                }
            });

        } else {
            // PLAYER CONTROLS
            pendingRequestsSection.setVisibility(View.GONE);
            btnPrimaryAction.setText("LEAVE TABLE");
            btnPrimaryAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F"))); // Red

            btnPrimaryAction.setOnClickListener(v -> {
                Toast.makeText(this, "You have left the table.", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }

    private void updateHostPrimaryButton() {
        if (!sessionStarted) {
            btnPrimaryAction.setText("START SESSION");
            btnPrimaryAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green
        } else {
            btnPrimaryAction.setText("END SESSION");
            btnPrimaryAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F"))); // Red
        }
    }

    private void addPlayerToRoster(String playerName, boolean isHostRole, boolean isPendingRequest) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 16, 0, 16);

        // QUICK-PEEK PROFILE TRIGGER
        row.setOnClickListener(v -> showQuickPeekProfile(playerName));

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvName.setText(playerName);
        tvName.setTextColor(Color.parseColor("#2D2B30"));
        tvName.setTextSize(16);
        row.addView(tvName);

        if (isPendingRequest) {
            // Add Accept / Deny Buttons for Pending Requests
            Button btnAccept = new Button(this);
            btnAccept.setText("✓");
            btnAccept.setTextSize(18);
            btnAccept.setTextColor(Color.WHITE);
            btnAccept.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            btnAccept.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
            btnAccept.setOnClickListener(v -> {
                Toast.makeText(this, "Accepted " + playerName, Toast.LENGTH_SHORT).show();
                pendingListContainer.removeView(row);
                addPlayerToRoster(playerName, false, false); // Move to active
            });
            row.addView(btnAccept);

            Button btnDeny = new Button(this);
            btnDeny.setText("✕");
            btnDeny.setTextSize(14);
            btnDeny.setTextColor(Color.WHITE);
            btnDeny.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
            LinearLayout.LayoutParams denyParams = new LinearLayout.LayoutParams(100, 100);
            denyParams.setMargins(16, 0, 0, 0);
            btnDeny.setLayoutParams(denyParams);
            btnDeny.setOnClickListener(v -> {
                Toast.makeText(this, "Denied " + playerName, Toast.LENGTH_SHORT).show();
                pendingListContainer.removeView(row);
            });
            row.addView(btnDeny);

            pendingListContainer.addView(row);

        } else {
            // Active Roster Logic
            if (isHostRole) {
                TextView tvHostBadge = new TextView(this);
                tvHostBadge.setText("[Host]");
                tvHostBadge.setTextColor(Color.parseColor("#FC7A57"));
                tvHostBadge.setTypeface(null, Typeface.BOLD);
                row.addView(tvHostBadge);
            } else if (isHost) {
                // If I am the Host looking at a standard player, show the Kick button
                TextView tvKick = new TextView(this);
                tvKick.setText("KICK");
                tvKick.setTextColor(Color.parseColor("#888888"));
                tvKick.setTextSize(12);
                tvKick.setPadding(16, 8, 16, 8);
                tvKick.setOnClickListener(v -> {
                    Toast.makeText(this, "Kicked " + playerName, Toast.LENGTH_SHORT).show();
                    activeRosterContainer.removeView(row);
                });
                row.addView(tvKick);
            }
            activeRosterContainer.addView(row);
        }
    }

    private void showQuickPeekProfile(String playerName) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_player_profile, null);
        TextView dialogName = dialogView.findViewById(R.id.dialogName);
        dialogName.setText(playerName);

        AlertDialog profileDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (profileDialog.getWindow() != null) {
            profileDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        profileDialog.show();
    }
}