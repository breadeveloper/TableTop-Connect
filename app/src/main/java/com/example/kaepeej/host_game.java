package com.example.kaepeej;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class host_game extends AppCompatActivity {

    private MapView miniMap;
    private Marker pinMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvPlayerCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSMDroid Configuration (Must be before setContentView)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_host_game);

        // Initialize Google Fused Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 1. Handle System Bar Padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Initialize UI Views
        ImageButton btnBack = findViewById(R.id.btnBack);
        AutoCompleteTextView autoCompleteCategory = findViewById(R.id.autoCompleteCategory);
        Slider sliderPlayers = findViewById(R.id.sliderPlayers);
        tvPlayerCount = findViewById(R.id.tvPlayerCount);
        miniMap = findViewById(R.id.miniMap);
        MaterialButton btnGetLocation = findViewById(R.id.btnGetLocation);
        MaterialButton btnCreateLobby = findViewById(R.id.btnCreateLobby);

        // 3. Setup Back Button
        btnBack.setOnClickListener(v -> finish());

        // 4. Setup Category Dropdown
        String[] categories = {"Strategy", "Party Game", "Card Game", "RPG", "Family", "Adventure"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        autoCompleteCategory.setAdapter(adapter);

        // 5. Setup Player Slider
        sliderPlayers.addOnChangeListener((slider, value, fromUser) -> {
            tvPlayerCount.setText(String.valueOf((int) value));
        });

        // 6. Setup Mini Map
        setupMiniMap();

        // 7. GPS Button Logic
        btnGetLocation.setOnClickListener(v -> requestGPSLocation());

        // 8. Launch Button Logic
        btnCreateLobby.setOnClickListener(v -> {
            String lat = String.valueOf(pinMarker.getPosition().getLatitude());
            String lon = String.valueOf(pinMarker.getPosition().getLongitude());
            Toast.makeText(this, "SQUAD LAUNCHED at: " + lat + ", " + lon, Toast.LENGTH_LONG).show();
            // Here you would normally save to Firebase/Database
            finish();
        });
    }

    private void setupMiniMap() {
        miniMap.setMultiTouchControls(true);
        miniMap.getController().setZoom(17.0);

        // Default to Daet Center
        GeoPoint daetPoint = new GeoPoint(14.1122, 122.9550);
        miniMap.getController().setCenter(daetPoint);

        // Add the Pin Marker
        pinMarker = new Marker(miniMap);
        pinMarker.setPosition(daetPoint);
        pinMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        pinMarker.setTitle("Venue Location");
        pinMarker.setDraggable(true);
        miniMap.getOverlays().add(pinMarker);

        // IMPORTANT: Allow map to scroll even inside a NestedScrollView
        miniMap.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        // Tap on Map to Move Pin
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                pinMarker.setPosition(p);
                miniMap.invalidate(); // Refresh map
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        };
        miniMap.getOverlays().add(new MapEventsOverlay(mReceive));
    }

    private void requestGPSLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                pinMarker.setPosition(userLocation);
                miniMap.getController().animateTo(userLocation);
                Toast.makeText(this, "Location pinned to your GPS", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Could not find GPS. Try moving outside.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Permission Result Handling
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestGPSLocation();
        }
    }

    // Required OSMDroid Lifecycle overrides
    @Override
    public void onResume() {
        super.onResume();
        miniMap.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        miniMap.onPause();
    }
}