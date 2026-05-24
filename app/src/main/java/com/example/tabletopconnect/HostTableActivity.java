package com.example.tabletopconnect;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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

// --- FIREBASE & UI IMPORTS ---
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HostTableActivity extends AppCompatActivity {

    private int playerCount = 4;
    private boolean isPinLocked = false;

    // UI & Map Elements
    private Button btnTogglePin;
    private MapView map;
    private Marker venueMarker;
    private MyLocationNewOverlay locationOverlay;

    // Schedule Elements
    private SwitchMaterial switchSchedule;
    private TextInputEditText inputDate, inputStartTime, inputEndTime;

    // Time-Travel Blocker Calendars
    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm a", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_host_table);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        ImageView backBtn = findViewById(R.id.backArrow);
        backBtn.setOnClickListener(v -> finish());

        com.google.android.material.slider.Slider playerSlider = findViewById(R.id.playerSlider);
        TextView txtCount = findViewById(R.id.sliderValueText);

        playerSlider.addOnChangeListener((slider, value, fromUser) -> {
            playerCount = (int) value;
            txtCount.setText(playerCount + " Players");
        });

        // --- UPGRADED MAP SETUP ---
        map = findViewById(R.id.venueMap);
        map.setMultiTouchControls(true);

        // Apply clean, minimalist CartoLight map tiles
        map.setTileSource(new XYTileSource(
                "CartoLight",
                0, 20, 256, ".png",
                new String[]{"https://a.basemaps.cartocdn.com/light_all/"}
        ));

        map.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        GeoPoint startPoint = new GeoPoint(14.1153, 122.9546);
        map.getController().setZoom(18.0);
        map.getController().setCenter(startPoint);

        // Apply Custom Orange Pin
        venueMarker = new Marker(map);
        venueMarker.setPosition(startPoint);
        venueMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        // Using 40x40 here so it is distinct and easy to grab on the Host screen
        venueMarker.setIcon(getScaledPin(R.drawable.orangepin, 40, 40));
        map.getOverlays().add(venueMarker);

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

        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
        locationOverlay.setDrawAccuracyEnabled(false);
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
                Toast.makeText(this, "Searching for GPS signal...", Toast.LENGTH_LONG).show();
            }
            isPinLocked = false;
            updatePinUI();
        });

        // --- SCHEDULE LOGIC ---
        switchSchedule = findViewById(R.id.switchSchedule);
        inputDate = findViewById(R.id.inputDate);
        inputStartTime = findViewById(R.id.inputStartTime);
        inputEndTime = findViewById(R.id.inputEndTime);

        // Toggle Enabled State & Colors
        switchSchedule.setOnCheckedChangeListener((buttonView, isChecked) -> {

            // 1. Lock/Unlock the text fields
            inputDate.setEnabled(!isChecked);
            inputStartTime.setEnabled(!isChecked);
            inputEndTime.setEnabled(!isChecked);

            // 2. Handle State Changes
            if (isChecked) {
                // ACTIVE STATE (Play Now)
                inputDate.setText("");
                inputStartTime.setText("");
                inputEndTime.setText("");

                // Turn Switch to Coral
                switchSchedule.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#FC7A57")));
                switchSchedule.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#FFCCBC")));
            } else {
                // DISABLED STATE (Schedule for Later)
                // Turn Switch to Dark Olive/Gray
                switchSchedule.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#5E5B52")));
                switchSchedule.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            }
        });

        inputDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                startCalendar.set(Calendar.YEAR, year);
                startCalendar.set(Calendar.MONTH, month);
                startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                endCalendar.set(Calendar.YEAR, year);
                endCalendar.set(Calendar.MONTH, month);
                endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                inputDate.setText(dateFormatter.format(startCalendar.getTime()));
            }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        inputStartTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                startCalendar.set(Calendar.MINUTE, minute);
                inputStartTime.setText(timeFormatter.format(startCalendar.getTime()));
            }, startCalendar.get(Calendar.HOUR_OF_DAY), startCalendar.get(Calendar.MINUTE), false).show();
        });

        inputEndTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                endCalendar.set(Calendar.MINUTE, minute);
                inputEndTime.setText(timeFormatter.format(endCalendar.getTime()));
            }, endCalendar.get(Calendar.HOUR_OF_DAY), endCalendar.get(Calendar.MINUTE), false).show();
        });

        // --- LAUNCH TABLE BUTTON ---
        Button launchBtn = findViewById(R.id.launchSquadButton);
        launchBtn.setOnClickListener(v -> {

            TextInputEditText gameInput = findViewById(R.id.hostGameNameInput);
            String gameName = gameInput.getText().toString().trim();
            if (gameName.isEmpty()) {
                Toast.makeText(this, "Please enter a game name!", Toast.LENGTH_SHORT).show();
                return;
            }

            TextInputEditText venueInput = findViewById(R.id.hostVenueInput);
            String venueName = venueInput.getText().toString().trim();
            if (venueName.isEmpty()) {
                Toast.makeText(this, "Please enter a venue name!", Toast.LENGTH_SHORT).show();
                return;
            }

            ChipGroup genreGroup = findViewById(R.id.genreChipGroup);
            List<String> selectedCategories = new ArrayList<>();
            for (int id : genreGroup.getCheckedChipIds()) {
                Chip chip = findViewById(id);
                if (chip != null) selectedCategories.add(chip.getText().toString());
            }
            if (selectedCategories.isEmpty()) {
                Toast.makeText(this, "Please select at least one category!", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- NEW: EXTRACT JOIN METHOD ---
            RadioGroup rgJoinMethod = findViewById(R.id.rgJoinMethod);
            String joinMethod = "Immediate"; // Default
            if (rgJoinMethod.getCheckedRadioButtonId() == R.id.rbRequestJoin) {
                joinMethod = "Request";
            }

            String status = "Open";
            String dateString = "";
            String startString = "";
            String endString = "";

            if (!switchSchedule.isChecked()) {
                status = "Scheduled";
                dateString = inputDate.getText().toString();
                startString = inputStartTime.getText().toString();
                endString = inputEndTime.getText().toString();

                if (dateString.isEmpty() || startString.isEmpty() || endString.isEmpty()) {
                    Toast.makeText(this, "Please fill out all schedule fields!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (endCalendar.before(startCalendar) || endCalendar.equals(startCalendar)) {
                    Toast.makeText(this, "End time must be after Start time!", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            double finalLat = venueMarker.getPosition().getLatitude();
            double finalLon = venueMarker.getPosition().getLongitude();
            String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(finalLat, finalLon));

            Map<String, Object> tableData = new HashMap<>();
            tableData.put("gameName", gameName);
            tableData.put("venueName", venueName);
            tableData.put("categories", selectedCategories);
            tableData.put("playerCount", playerCount);
            tableData.put("latitude", finalLat);
            tableData.put("longitude", finalLon);
            tableData.put("geohash", hash);
            tableData.put("timestamp", System.currentTimeMillis());
            tableData.put("status", status);
            tableData.put("scheduledDate", dateString);
            tableData.put("startTime", startString);
            tableData.put("endTime", endString);

            // Push our new Join Method!
            tableData.put("joinMethod", joinMethod);

            launchBtn.setEnabled(false);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("active_tables")
                    .add(tableData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Table Live! Squad assembled.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(HostTableActivity.this, HomeActivity.class);
                        intent.putExtra("OPEN_TAB", 0);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Network error. Failed to launch table.", Toast.LENGTH_LONG).show();
                        launchBtn.setEnabled(true);
                    });
        });
    }

    // Helper method to scale the custom pin
    private Drawable getScaledPin(int drawableResId, int width, int height) {
        Drawable baseDrawable = ContextCompat.getDrawable(this, drawableResId);
        if (baseDrawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) baseDrawable).getBitmap();
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            return new BitmapDrawable(getResources(), scaledBitmap);
        }
        return baseDrawable;
    }

    // Upgraded Button Toggle UI
    private void updatePinUI() {
        if (isPinLocked) {
            btnTogglePin.setText("PIN LOCKED");
            // Dark Olive/Gray when disabled
            btnTogglePin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#5E5B52")));
            btnTogglePin.setTextColor(Color.WHITE);
        } else {
            btnTogglePin.setText("EDIT PIN");
            // Coral when enabled and active
            btnTogglePin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FC7A57")));
            btnTogglePin.setTextColor(Color.WHITE);
            Toast.makeText(this, "Tap anywhere on the map to drop the pin.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() { super.onResume(); if (map != null) map.onResume(); }

    @Override
    public void onPause() { super.onPause(); if (map != null) map.onPause(); }
}