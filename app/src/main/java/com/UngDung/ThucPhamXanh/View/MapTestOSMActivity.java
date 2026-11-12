package com.UngDung.ThucPhamXanh.View;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.UngDung.ThucPhamXanh.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapTestOSMActivity extends AppCompatActivity {

    private static final String TAG = "MapTestOSMActivity";
    private MapView mapView;
    private IMapController mapController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_test_osm);

        Log.d(TAG, "MapTestOSMActivity created");

        // Cấu hình OpenStreetMap
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));

        initMap();

        Button btnTest = findViewById(R.id.btn_test);
        btnTest.setOnClickListener(v -> {
            if (mapView != null) {
                Toast.makeText(this, "OpenStreetMap is working!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "OpenStreetMap is working!");
            } else {
                Toast.makeText(this, "Map is null!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Map is null!");
            }
        });
    }

    private void initMap() {
        mapView = findViewById(R.id.map_view);

        if (mapView == null) {
            Log.e(TAG, "MapView is null!");
            return;
        }

        // Cấu hình map
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(15);

        // Add a marker in Ho Chi Minh City
        GeoPoint hcmc = new GeoPoint(10.8231, 106.6297);
        Marker marker = new Marker(mapView);
        marker.setPosition(hcmc);
        marker.setTitle("Ho Chi Minh City");
        mapView.getOverlays().add(marker);

        mapController = mapView.getController();
        mapController.setCenter(hcmc);

        Log.d(TAG, "OpenStreetMap initialized successfully");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
}
