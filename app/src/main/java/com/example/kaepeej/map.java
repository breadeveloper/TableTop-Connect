package com.example.kaepeej;import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class map extends AppCompatActivity {

    private MapView mapView = null;
    private MaterialCardView cvGameDetail;
    private RecyclerView rvHostList;
    private MaterialButton btnSwitchView;
    private boolean isListView = false; // Track which view is showing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map);

        // 1. Initialize Map
        mapView = findViewById(R.id.map);
        mapView.setMultiTouchControls(true);
        GeoPoint daetPoint = new GeoPoint(14.1122, 122.9550);
        mapView.getController().setZoom(16.0);
        mapView.getController().setCenter(daetPoint);

        // 2. Initialize UI Elements
        ImageView btnBack = findViewById(R.id.btnBack);
        cvGameDetail = findViewById(R.id.cvGameDetail);
        btnSwitchView = findViewById(R.id.btnSwitchView);
        rvHostList = findViewById(R.id.rvHostList);
        MaterialButton btnJoinLobby = findViewById(R.id.btnJoinLobby);

        // Setup RecyclerView (The list container)
        rvHostList.setLayoutManager(new LinearLayoutManager(this));

        // 3. Handle System Bar Padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 4. Back Button
        btnBack.setOnClickListener(v -> finish());

        // 5. Join Lobby Logic
        btnJoinLobby.setOnClickListener(v -> {
            startActivity(new Intent(map.this, chatroom.class));
        });

        // 6. TOGGLE VIEW LOGIC (Switch between Map and List)
        btnSwitchView.setOnClickListener(v -> {
            if (!isListView) {
                // Switch to List View
                mapView.setVisibility(View.GONE);
                cvGameDetail.setVisibility(View.GONE); // Hide detail card if open
                rvHostList.setVisibility(View.VISIBLE);
                btnSwitchView.setText("SHOW MAP");
                isListView = true;
            } else {
                // Switch back to Map View
                rvHostList.setVisibility(View.GONE);
                mapView.setVisibility(View.VISIBLE);
                btnSwitchView.setText("SHOW LIST");
                isListView = false;
            }
        });

        // 7. Add Marker
        addGameMarker(14.1122, 122.9550, "Catan Night", "Daet Central Plaza");
    }

    private void addGameMarker(double lat, double lon, String title, String host) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);

        marker.setOnMarkerClickListener((m, mv) -> {
            if (!isListView) {
                cvGameDetail.setVisibility(View.VISIBLE);
            }
            return true;
        });

        mapView.getOverlays().add(marker);
    }

    @Override
    public void onResume() { super.onResume(); mapView.onResume(); }
    @Override
    public void onPause() { super.onPause(); mapView.onPause(); }
}