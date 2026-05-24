package com.example.tabletopconnect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

// --- MAP IMPORTS ---
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

// --- FIREBASE IMPORTS ---
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FindTableActivity extends AppCompatActivity {

    private MapView map;
    private MyLocationNewOverlay locationOverlay;

    // UI Elements
    private CardView cvGameDetail;
    private TextView bottomSheetGameName, bottomSheetVenueName, bottomSheetPlayerCount;
    private TextView tvStatusText, tvScheduleText;
    private ImageView ivStatusCircle;
    private EditText etSearchTable;
    private ImageButton btnFilter;

    // Data Management
    private List<QueryDocumentSnapshot> allTables = new ArrayList<>();
    private String currentSearchText = "";

    // QoL Search Mechanics
    private List<Marker> visibleMarkers = new ArrayList<>();
    private int currentSearchCycleIndex = 0;
    private Marker currentlySelectedMarker = null;

    // Filter Arrays & Preferences
    private String[] statuses = {"Open", "Closed", "Ongoing", "Scheduled", "Ended"};
    private boolean[] selectedStatuses = {true, false, true, true, false};

    // NEW: Player Preference Trackers
    private boolean filterHideFull = false;
    private boolean filterJoinImmediate = true;
    private boolean filterRequestJoin = true;

    private String[] categories = {
            "Card", "Dice", "Economic", "Bluffing", "Farming", "Abstract Strategy",
            "Murder Mystery", "Children's", "Educational", "Engine Building", "Deduction",
            "Memory", "Word", "Math-Based", "Collectible", "Racing", "RPG", "Legacy",
            "Worker Placement", "Train", "Party", "Co-op"
    };
    private boolean[] selectedCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_find_table);

        selectedCategories = new boolean[categories.length];
        java.util.Arrays.fill(selectedCategories, true);

        // Link UI
        ImageView btnBack = findViewById(R.id.btnBackFind);
        cvGameDetail = findViewById(R.id.cvGameDetail);
        bottomSheetGameName = findViewById(R.id.bottomSheetGameName);
        bottomSheetVenueName = findViewById(R.id.bottomSheetVenueName);
        bottomSheetPlayerCount = findViewById(R.id.bottomSheetPlayerCount);

        tvStatusText = findViewById(R.id.tvStatusText);
        tvScheduleText = findViewById(R.id.tvScheduleText);
        ivStatusCircle = findViewById(R.id.ivStatusCircle);

        etSearchTable = findViewById(R.id.etSearchTable);
        btnFilter = findViewById(R.id.btnFilter);

        btnBack.setOnClickListener(v -> finish());

        // Setup Map
        map = findViewById(R.id.findMapView);
        map.setMultiTouchControls(true);
        map.setTileSource(new XYTileSource("CartoLight", 0, 20, 256, ".png", new String[]{"https://a.basemaps.cartocdn.com/light_all/"}));

        GeoPoint startPoint = new GeoPoint(14.1153, 122.9546);
        map.getController().setZoom(16.0);
        map.getController().setCenter(startPoint);

        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                cvGameDetail.setVisibility(View.GONE);
                resetSelectedMarker();
                return true;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        };
        map.getOverlays().add(new MapEventsOverlay(mReceive));

        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
        locationOverlay.setDrawAccuracyEnabled(false);
        map.getOverlays().add(locationOverlay);

        locationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            GeoPoint myLocation = locationOverlay.getMyLocation();
            if (myLocation != null) {
                map.getController().animateTo(myLocation);
                map.getController().setZoom(18.0);
            }
        }));

        // --- QoL Search Logic (Enter Key Cycling) ---
        etSearchTable.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString().toLowerCase().trim();
                applyFilters();
                currentSearchCycleIndex = 0;

                if (visibleMarkers.size() == 1) {
                    triggerMarkerClick(visibleMarkers.get(0));
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearchTable.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                if (visibleMarkers.size() > 1) {
                    triggerMarkerClick(visibleMarkers.get(currentSearchCycleIndex));
                    currentSearchCycleIndex++;
                    if (currentSearchCycleIndex >= visibleMarkers.size()) currentSearchCycleIndex = 0;
                }
                return true;
            }
            return false;
        });

        btnFilter.setOnClickListener(v -> showCustomFilterDialog());
        fetchActiveTables();
    }

    // --- PIN & COLOR LOGIC ---
    private Drawable getScaledPin(int drawableResId, int size) {
        Drawable baseDrawable = ContextCompat.getDrawable(this, drawableResId);
        if (baseDrawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) baseDrawable).getBitmap();
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
            return new BitmapDrawable(getResources(), scaledBitmap);
        }
        return baseDrawable;
    }

    private int getPinDrawableForStatus(String status) {
        if (status == null) return R.drawable.greenpin;
        switch (status) {
            case "Closed": return R.drawable.redpin;
            case "Ongoing": return R.drawable.yellowpin;
            case "Scheduled": return R.drawable.bluepin;
            case "Ended": return R.drawable.orangepin;
            default: return R.drawable.greenpin;
        }
    }

    private String getHexColorForStatus(String status) {
        if (status == null) return "#4CAF50";
        switch (status) {
            case "Closed": return "#F44336";
            case "Ongoing": return "#FCD757";
            case "Scheduled": return "#2196F3";
            case "Ended": return "#FC7A57";
            default: return "#4CAF50";
        }
    }

    private void resetSelectedMarker() {
        if (currentlySelectedMarker != null) {
            String savedStatus = (String) currentlySelectedMarker.getRelatedObject();
            currentlySelectedMarker.setIcon(getScaledPin(getPinDrawableForStatus(savedStatus), 25));
            currentlySelectedMarker = null;
            map.invalidate();
        }
    }

    private void triggerMarkerClick(Marker clickedMarker) {
        resetSelectedMarker();

        String clickedStatus = (String) clickedMarker.getRelatedObject();
        clickedMarker.setIcon(getScaledPin(getPinDrawableForStatus(clickedStatus), 35));
        currentlySelectedMarker = clickedMarker;

        bottomSheetGameName.setText(clickedMarker.getTitle());
        bottomSheetVenueName.setText(clickedMarker.getSnippet());
        bottomSheetPlayerCount.setText(clickedMarker.getSubDescription());

        tvStatusText.setText(clickedStatus);
        ivStatusCircle.setColorFilter(Color.parseColor(getHexColorForStatus(clickedStatus)));

        tvScheduleText.setText(clickedMarker.getId());

        cvGameDetail.setVisibility(View.VISIBLE);
        map.getController().animateTo(clickedMarker.getPosition());
        map.invalidate();
    }

    // --- UPGRADED 3-TIER FILTER DIALOG ---
    private void showCustomFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Radar Filters");

        // Temporary arrays
        boolean[] tempStatuses = java.util.Arrays.copyOf(selectedStatuses, selectedStatuses.length);
        boolean[] tempCategories = java.util.Arrays.copyOf(selectedCategories, selectedCategories.length);

        // Temporary preferences
        final boolean[] tempPrefs = {filterHideFull, filterJoinImmediate, filterRequestJoin};

        ScrollView scroll = new ScrollView(this);
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(48, 24, 48, 24);

        // --- SECTION 1: TABLE STATUS ---
        TextView txtStatusTitle = new TextView(this);
        txtStatusTitle.setText("Table Status");
        txtStatusTitle.setTextSize(16);
        txtStatusTitle.setTextColor(Color.parseColor("#FC7A57"));
        txtStatusTitle.setPadding(0, 0, 0, 16);
        mainLayout.addView(txtStatusTitle);

        for (int i = 0; i < statuses.length; i++) {
            CheckBox cb = new CheckBox(this);
            cb.setText(statuses[i]);
            cb.setChecked(tempStatuses[i]);
            final int index = i;
            cb.setOnCheckedChangeListener((btn, isChecked) -> tempStatuses[index] = isChecked);
            mainLayout.addView(cb);
        }

        // Divider 1
        View divider1 = new View(this);
        divider1.setBackgroundColor(Color.parseColor("#E0E0E0"));
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
        params1.setMargins(0, 32, 0, 32);
        mainLayout.addView(divider1, params1);

        // --- SECTION 2: PLAYER PREFERENCES ---
        TextView txtPrefsTitle = new TextView(this);
        txtPrefsTitle.setText("Player Preferences");
        txtPrefsTitle.setTextSize(16);
        txtPrefsTitle.setTextColor(Color.parseColor("#FC7A57"));
        txtPrefsTitle.setPadding(0, 0, 0, 16);
        mainLayout.addView(txtPrefsTitle);

        CheckBox cbHideFull = new CheckBox(this);
        cbHideFull.setText("Hide Full Tables");
        cbHideFull.setChecked(tempPrefs[0]);
        cbHideFull.setOnCheckedChangeListener((btn, isChecked) -> tempPrefs[0] = isChecked);
        mainLayout.addView(cbHideFull);

        CheckBox cbJoinImm = new CheckBox(this);
        cbJoinImm.setText("Join Immediately");
        cbJoinImm.setChecked(tempPrefs[1]);
        cbJoinImm.setOnCheckedChangeListener((btn, isChecked) -> tempPrefs[1] = isChecked);
        mainLayout.addView(cbJoinImm);

        CheckBox cbReqJoin = new CheckBox(this);
        cbReqJoin.setText("Request to Join");
        cbReqJoin.setChecked(tempPrefs[2]);
        cbReqJoin.setOnCheckedChangeListener((btn, isChecked) -> tempPrefs[2] = isChecked);
        mainLayout.addView(cbReqJoin);

        // Divider 2
        View divider2 = new View(this);
        divider2.setBackgroundColor(Color.parseColor("#E0E0E0"));
        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
        params2.setMargins(0, 32, 0, 32);
        mainLayout.addView(divider2, params2);

        // --- SECTION 3: GAME CATEGORIES ---
        TextView txtCatTitle = new TextView(this);
        txtCatTitle.setText("Game Categories");
        txtCatTitle.setTextSize(16);
        txtCatTitle.setTextColor(Color.parseColor("#FC7A57"));
        txtCatTitle.setPadding(0, 0, 0, 16);
        mainLayout.addView(txtCatTitle);

        List<CheckBox> categoryBoxes = new ArrayList<>();
        for (int i = 0; i < categories.length; i++) {
            CheckBox cb = new CheckBox(this);
            cb.setText(categories[i]);
            cb.setChecked(tempCategories[i]);
            final int index = i;
            cb.setOnCheckedChangeListener((btn, isChecked) -> tempCategories[index] = isChecked);
            categoryBoxes.add(cb);
            mainLayout.addView(cb);
        }

        scroll.addView(mainLayout);
        builder.setView(scroll);

        builder.setPositiveButton("Apply", (dialog, which) -> {
            // Save all states when they hit Apply
            selectedStatuses = java.util.Arrays.copyOf(tempStatuses, tempStatuses.length);
            selectedCategories = java.util.Arrays.copyOf(tempCategories, tempCategories.length);
            filterHideFull = tempPrefs[0];
            filterJoinImmediate = tempPrefs[1];
            filterRequestJoin = tempPrefs[2];
            applyFilters();
        });

        builder.setNegativeButton("Cancel", null);
        builder.setNeutralButton("Clear Categories", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Smart Toggle (Only affects Categories!)
        Button neutralBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        boolean hasCats = false;
        for (boolean b : tempCategories) { if (b) { hasCats = true; break; } }
        neutralBtn.setText(hasCats ? "Clear Categories" : "Select Categories");

        neutralBtn.setOnClickListener(v -> {
            boolean currentHasCats = false;
            for (boolean b : tempCategories) { if (b) { currentHasCats = true; break; } }
            boolean newState = !currentHasCats;

            java.util.Arrays.fill(tempCategories, newState);
            for (CheckBox cb : categoryBoxes) { cb.setChecked(newState); }
            neutralBtn.setText(newState ? "Clear Categories" : "Select Categories");
        });
    }

    private void fetchActiveTables() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("active_tables").get().addOnSuccessListener(queryDocumentSnapshots -> {
            allTables.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                allTables.add(document);
            }
            applyFilters();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading radar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void applyFilters() {
        for (int i = map.getOverlays().size() - 1; i >= 0; i--) {
            if (map.getOverlays().get(i) instanceof Marker) {
                map.getOverlays().remove(i);
            }
        }
        currentlySelectedMarker = null;
        visibleMarkers.clear();

        for (QueryDocumentSnapshot document : allTables) {
            String gameName = document.getString("gameName");
            String venueName = document.getString("venueName");
            String status = document.getString("status");
            String joinMethod = document.getString("joinMethod");
            List<String> tableCategories = (List<String>) document.get("categories");

            // Legacy Database Support
            if (status == null) status = "Open";
            if (joinMethod == null) joinMethod = "Immediate";
            if (gameName == null) gameName = "";
            if (venueName == null) venueName = "";

            // 1. Search Filter
            boolean matchesSearch = gameName.toLowerCase().contains(currentSearchText) || venueName.toLowerCase().contains(currentSearchText);

            // 2. Status Filter
            boolean matchesStatus = false;
            for (int i = 0; i < statuses.length; i++) {
                if (selectedStatuses[i] && status.equalsIgnoreCase(statuses[i])) {
                    matchesStatus = true;
                    break;
                }
            }

            // 3. Category Filter
            boolean matchesCategory = false;
            if (tableCategories == null || tableCategories.isEmpty()) {
                matchesCategory = true;
            } else {
                for (int i = 0; i < categories.length; i++) {
                    if (selectedCategories[i] && tableCategories.contains(categories[i])) {
                        matchesCategory = true;
                        break;
                    }
                }
            }

            // 4. Preferences Filter: Hide Full Tables
            boolean passesFullCheck = true;
            Long maxPlayers = document.getLong("playerCount");
            Long currentPlayers = document.getLong("currentPlayers");
            if (currentPlayers == null) currentPlayers = 1L; // Fallback
            if (filterHideFull && maxPlayers != null && currentPlayers >= maxPlayers) {
                passesFullCheck = false;
            }

            // 5. Preferences Filter: Join Method
            boolean matchesJoinMethod = false;
            if (joinMethod.equals("Immediate") && filterJoinImmediate) matchesJoinMethod = true;
            if (joinMethod.equals("Request") && filterRequestJoin) matchesJoinMethod = true;

            // --- FINAL VALIDATION ---
            if (matchesSearch && matchesStatus && matchesCategory && passesFullCheck && matchesJoinMethod) {
                Double lat = document.getDouble("latitude");
                Double lon = document.getDouble("longitude");
                String displayPlayers = (maxPlayers != null) ? currentPlayers + "/" + maxPlayers.toString() : "?";

                if (lat != null && lon != null) {
                    GeoPoint tablePoint = new GeoPoint(lat, lon);
                    Marker tableMarker = new Marker(map);
                    tableMarker.setPosition(tablePoint);

                    tableMarker.setIcon(getScaledPin(getPinDrawableForStatus(status), 25));
                    tableMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    tableMarker.setRelatedObject(status);

                    tableMarker.setTitle(gameName);
                    tableMarker.setSnippet(venueName);
                    tableMarker.setSubDescription(displayPlayers);

                    String schedDate = document.getString("scheduledDate");
                    String schedTime = document.getString("startTime");
                    String finalSchedText = "";
                    if (status.equals("Scheduled") && schedDate != null && !schedDate.isEmpty()) {
                        finalSchedText = schedDate + ", " + schedTime;
                    } else if (status.equals("Ended")) {
                        finalSchedText = "Session Finished";
                    } else {
                        finalSchedText = "Playing Now";
                    }
                    tableMarker.setId(finalSchedText);

                    tableMarker.setOnMarkerClickListener((clickedMarker, mapView) -> {
                        triggerMarkerClick(clickedMarker);
                        return true;
                    });

                    map.getOverlays().add(tableMarker);
                    visibleMarkers.add(tableMarker);
                }
            }
        }
        map.invalidate();
    }

    @Override
    public void onResume() { super.onResume(); if (map != null) map.onResume(); }
    @Override
    public void onPause() { super.onPause(); if (map != null) map.onPause(); }
}