package com.UngDung.ThucPhamXanh.View;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.UngDung.ThucPhamXanh.R;

public class MapTestActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapTestActivity";
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_test);

        Log.d(TAG, "MapTestActivity created");

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            Log.d(TAG, "Map fragment found, getting map async...");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment is null!");
        }

        Button btnTest = findViewById(R.id.btn_test);
        btnTest.setOnClickListener(v -> {
            if (mMap != null) {
                Toast.makeText(this, "Map is working!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Map is working!");
            } else {
                Toast.makeText(this, "Map is null!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Map is null!");
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady called - Map is ready!");
        mMap = googleMap;

        if (mMap == null) {
            Log.e(TAG, "Map is null!");
            return;
        }

        // Add a marker in Ho Chi Minh City
        LatLng hcmc = new LatLng(10.8231, 106.6297);
        mMap.addMarker(new MarkerOptions()
                .position(hcmc)
                .title("Ho Chi Minh City"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hcmc, 15));

        Log.d(TAG, "Map initialized successfully");
    }
}
