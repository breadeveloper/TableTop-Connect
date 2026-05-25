package com.example.tabletopconnect;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MySessionsFragment extends Fragment {

    private LinearLayout sessionListContainer;
    private EditText etSearchSessions;
    private ImageButton btnFilterSessions;

    // Filter and Search State
    private List<MockSession> allSessions = new ArrayList<>();
    private String currentSearchQuery = "";
    private int currentFilterRole = 0; // 0 = All, 1 = Hosting, 2 = Playing

    // Simple Data Class for the Mock UI
    private static class MockSession {
        String gameName, venue, lastMessage;
        boolean isHost, needsReview;

        MockSession(String gameName, String venue, String lastMessage, boolean isHost, boolean needsReview) {
            this.gameName = gameName;
            this.venue = venue;
            this.lastMessage = lastMessage;
            this.isHost = isHost;
            this.needsReview = needsReview;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_sessions, container, false);

        sessionListContainer = view.findViewById(R.id.sessionListContainer);
        etSearchSessions = view.findViewById(R.id.etSearchSessions);
        btnFilterSessions = view.findViewById(R.id.btnFilterSessions);

        loadMockSessions();

        // --- REAL-TIME SEARCH LOGIC ---
        etSearchSessions.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // --- FILTER DIALOG LOGIC ---
        btnFilterSessions.setOnClickListener(v -> showFilterDialog());

        applyFilters();

        return view;
    }

    private void showFilterDialog() {
        String[] options = {"All Sessions", "Hosting Only", "Playing Only"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Filter Roles")
                .setSingleChoiceItems(options, currentFilterRole, (dialog, which) -> {
                    currentFilterRole = which;
                    applyFilters();
                    dialog.dismiss();
                })
                .show();
    }

    private void applyFilters() {
        sessionListContainer.removeAllViews();

        for (MockSession s : allSessions) {
            // 1. Check Search Query
            boolean matchesSearch = s.gameName.toLowerCase().contains(currentSearchQuery) ||
                    s.venue.toLowerCase().contains(currentSearchQuery);

            // 2. Check Role Filter
            boolean matchesRole = true;
            if (currentFilterRole == 1 && !s.isHost) matchesRole = false;
            if (currentFilterRole == 2 && s.isHost) matchesRole = false;

            if (matchesSearch && matchesRole) {
                addSessionRow(s.gameName, s.venue, s.lastMessage, s.isHost, s.needsReview);
            }
        }
    }

    // --- PROGRAMMATIC ROW BUILDER ---
    private void addSessionRow(String gameName, String venue, String lastMessage, boolean isHost, boolean needsReview) {
        // Main Container for the Row
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(64, 32, 64, 32);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setClickable(true);
        row.setFocusable(true);

        // Click Logic routing
        row.setOnClickListener(v -> {
            if (needsReview) {
                // Launch the Post-Match Review Activity and pass the game name
                Intent intent = new Intent(getActivity(), ReviewSessionActivity.class);
                intent.putExtra("GAME_NAME", gameName);
                startActivity(intent);
            } else {
                // Launch the isolated Chat Activity and pass the game name
                Intent intent = new Intent(getActivity(), SessionChatActivity.class);
                intent.putExtra("GAME_NAME", gameName);
                startActivity(intent);
            }
        });

        // Top Line: Game Name & Host Badge
        LinearLayout titleRow = new LinearLayout(getContext());
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvGameName = new TextView(getContext());
        tvGameName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvGameName.setText(gameName);
        tvGameName.setTextColor(Color.parseColor("#2D2B30"));
        tvGameName.setTextSize(18);
        tvGameName.setTypeface(null, Typeface.BOLD);
        tvGameName.setMaxLines(1);
        tvGameName.setEllipsize(TextUtils.TruncateAt.END);
        titleRow.addView(tvGameName);

        if (isHost && !needsReview) {
            TextView tvHostBadge = new TextView(getContext());
            tvHostBadge.setText("HOST"); // Simplified!
            tvHostBadge.setTextColor(Color.parseColor("#FC7A57"));
            tvHostBadge.setTextSize(12);
            tvHostBadge.setTypeface(null, Typeface.BOLD);
            tvHostBadge.setPadding(16, 0, 0, 0);
            titleRow.addView(tvHostBadge);
        }

        row.addView(titleRow);

        // Subtitle: Venue
        TextView tvVenue = new TextView(getContext());
        tvVenue.setText("@ " + venue);
        tvVenue.setTextColor(Color.parseColor("#888888"));
        tvVenue.setTextSize(12);
        tvVenue.setPadding(0, 4, 0, 8);
        row.addView(tvVenue);

        // Dynamic Bottom Content: Review Required OR Last Message
        if (needsReview) {
            TextView tvActionRequired = new TextView(getContext());
            tvActionRequired.setText("Action Required: Rate Session!"); // Shortened!
            tvActionRequired.setTextColor(Color.parseColor("#D32F2F")); // Red Alert
            tvActionRequired.setTextSize(14);
            tvActionRequired.setTypeface(null, Typeface.BOLD);
            row.addView(tvActionRequired);
        } else {
            TextView tvMessage = new TextView(getContext());
            tvMessage.setText(lastMessage);
            tvMessage.setTextColor(Color.parseColor("#5E5B52"));
            tvMessage.setTextSize(14);
            tvMessage.setMaxLines(1);
            tvMessage.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(tvMessage);
        }

        sessionListContainer.addView(row);

        // Thin divider between chats
        View divider = new View(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
        params.setMargins(64, 0, 64, 0);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.parseColor("#F0F0F0"));
        sessionListContainer.addView(divider);
    }

    private void loadMockSessions() {
        allSessions.clear();
        allSessions.add(new MockSession("D&D: Curse of Strahd", "Local Game Store", "", false, true));
        allSessions.add(new MockSession("Marvels Rival Co-op", "Dasmarinas Street", "LocalPlayer_2: I'm bringing the chips!", true, false));
        allSessions.add(new MockSession("Twilight Imperium 4th Ed.", "John's Apartment", "GameMaster_Daet: See you all at 8 PM.", false, false));
        allSessions.add(new MockSession("Catan Quick Match", "Starbucks Downtown", "You: Anyone need wood?", false, false));
    }
}