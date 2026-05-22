package com.example.tabletopconnect;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

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

        setContentView(R.layout.activity_host_table); // Or activity_host_game based on your file name

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

        // FIX: Prevent ScrollView from stealing map drag gestures
        map.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        // Default fallback starting point (Daet) while waiting for GPS
        GeoPoint startPoint = new GeoPoint(14.1153, 122.9546);
        map.getController().setZoom(18.0); // 18.0 is a very close street-level zoom
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
                    map.invalidate(); // Redraw map
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

        // THE MAGIC: The millisecond the GPS finds you, fly the camera there automatically!
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

        // Button 1: Toggle Pin Lock
        btnTogglePin.setOnClickListener(v -> {
            isPinLocked = !isPinLocked;
            updatePinUI();
        });

        // Button 2: Current Location
        btnCurrentLocation.setOnClickListener(v -> {
            GeoPoint myLocation = locationOverlay.getMyLocation();
            if (myLocation != null) {
                // Instantly snap to the user's GPS coordinates
                map.getController().animateTo(myLocation);
                map.getController().setZoom(18.0);
                venueMarker.setPosition(myLocation);
                map.invalidate();
            } else {
                Toast.makeText(this, "Searching for GPS signal... Please ensure Location is turned on.", Toast.LENGTH_LONG).show();
            }

            // Automatically unlock the pin so they can drag it to exactly where they are sitting
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

    // Helper method to visually flip the pin button state
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

    // Clean up map resources when app is minimized
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