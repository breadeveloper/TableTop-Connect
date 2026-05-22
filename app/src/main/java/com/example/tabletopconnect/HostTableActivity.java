package com.example.tabletopconnect;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// --- MAP IMPORTS ---
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

// --- FIREBASE & UI IMPORTS ---
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HostTableActivity extends AppCompatActivity {

    private int playerCount = 4;
    private boolean isPinLocked = false;

    // UI & Map Elements
    private Button btnTogglePin;
    private MapView map;
    private Marker venueMarker;
    private MyLocationNewOverlay locationOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Initialize map configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_host_table); // Or activity_host_game based on your file naming

        // --- REQUEST LOCATION PERMISSIONS ---
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // --- BACK BUTTON ---
        ImageView backBtn = findViewById(R.id.backArrow);
        backBtn.setOnClickListener(v -> finish());

        // --- SLIDER LOGIC ---
        com.google.android.material.slider.Slider playerSlider = findViewById(R.id.playerSlider);
        TextView txtCount = findViewById(R.id.sliderValueText);

        playerSlider.addOnChangeListener((slider, value, fromUser) -> {
            playerCount = (int) value;
            txtCount.setText(playerCount + " Players");
        });

        // --- MAP SETUP ---
        map = findViewById(R.id.venueMap);
        map.setMultiTouchControls(true);

        // Prevent ScrollView from stealing map drag gestures
        map.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        // Default fallback starting point (Daet) while waiting for GPS
        GeoPoint startPoint = new GeoPoint(14.1153, 122.9546);
        map.getController().setZoom(18.0);
        map.getController().setCenter(startPoint);

        // Setup the physical Pin
        venueMarker = new Marker(map);
        venueMarker.setPosition(startPoint);
        venueMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(venueMarker);

        // Tap-to-Move Pin Logic
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (!isPinLocked) {
                    venueMarker.setPosition(p);
                    map.invalidate();
                } else {
                    Toast.makeText(HostTableActivity.this, "Pin is locked! Tap 'EDIT PIN' to move it.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        };
        map.getOverlays().add(new MapEventsOverlay(mReceive));

        // --- GPS AUTO-LOCATE LOGIC ---
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
        map.getOverlays().add(locationOverlay);

        locationOverlay.runOnFirstFix(() -> {
            runOnUiThread(() -> {
                GeoPoint myLocation = locationOverlay.getMyLocation();
                if (myLocation != null) {
                    map.getController().animateTo(myLocation);
                    map.getController().setZoom(18.0);
                    venueMarker.setPosition(myLocation);
                    map.invalidate();
                }
            });
        });

        // --- VENUE BUTTON LOGIC ---
        btnTogglePin = findViewById(R.id.btnTogglePin);
        Button btnCurrentLocation = findViewById(R.id.btnCurrentLocation);

        btnTogglePin.setOnClickListener(v -> {
            isPinLocked = !isPinLocked;
            updatePinUI();
        });

        btnCurrentLocation.setOnClickListener(v -> {
            GeoPoint myLocation = locationOverlay.getMyLocation();
            if (myLocation != null) {
                map.getController().animateTo(myLocation);
                map.getController().setZoom(18.0);
                venueMarker.setPosition(myLocation);
                map.invalidate();
            } else {
                Toast.makeText(this, "Searching for GPS signal... Please ensure Location is turned on.", Toast.LENGTH_LONG).show();
            }

            isPinLocked = false;
            updatePinUI();
        });

        // --- LAUNCH SQUAD BUTTON (FIREBASE INTEGRATION) ---
        Button launchBtn = findViewById(R.id.launchSquadButton);
        launchBtn.setOnClickListener(v -> {

            // 1. Validate Game Name
            TextInputEditText gameInput = findViewById(R.id.hostGameNameInput);
            String gameName = gameInput.getText().toString().trim();
            if (gameName.isEmpty()) {
                Toast.makeText(this, "Please enter a game name!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Validate Venue Name
            TextInputEditText venueInput = findViewById(R.id.hostVenueInput);
            String venueName = venueInput.getText().toString().trim();
            if (venueName.isEmpty()) {
                Toast.makeText(this, "Please enter a venue name!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Gather Categories
            ChipGroup genreGroup = findViewById(R.id.genreChipGroup);
            List<String> selectedCategories = new ArrayList<>();
            for (int id : genreGroup.getCheckedChipIds()) {
                Chip chip = findViewById(id);
                if (chip != null) {
                    selectedCategories.add(chip.getText().toString());
                }
            }
            if (selectedCategories.isEmpty()) {
                Toast.makeText(this, "Please select at least one category!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 4. Gather Coordinates & Geohash
            double finalLat = venueMarker.getPosition().getLatitude();
            double finalLon = venueMarker.getPosition().getLongitude();
            String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(finalLat, finalLon));

            // 5. Package into Database Object
            Map<String, Object> tableData = new HashMap<>();
            tableData.put("gameName", gameName);
            tableData.put("venueName", venueName);
            tableData.put("categories", selectedCategories);
            tableData.put("playerCount", playerCount);
            tableData.put("latitude", finalLat);
            tableData.put("longitude", finalLon);
            tableData.put("geohash", hash);
            tableData.put("timestamp", System.currentTimeMillis());

            // 6. Push to Firestore
            launchBtn.setEnabled(false);

            // --- YOUR NEW DEBUG TOAST ---
            Toast.makeText(this, "Fetched! Lat: " + finalLat + " | Lon: " + finalLon, Toast.LENGTH_LONG).show();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("active_tables")
                    .add(tableData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Table Live! Squad assembled.", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(HostTableActivity.this, MySessionsFragment.class);
                        startActivity(intent);

                        // We still call finish() so the user can't hit the "Back" button
                        // and accidentally launch the exact same table a second time!
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to launch: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        launchBtn.setEnabled(true);
                    });
        });
    }

    private void updatePinUI() {
        if (isPinLocked) {
            btnTogglePin.setText("PIN LOCKED");
            btnTogglePin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#5E5B52")));
            btnTogglePin.setTextColor(Color.WHITE);
        } else {
            btnTogglePin.setText("PIN UNLOCKED");
            btnTogglePin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E1BEE7")));
            btnTogglePin.setTextColor(Color.parseColor("#4A148C"));
            Toast.makeText(this, "Tap anywhere on the map to drop the pin.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}