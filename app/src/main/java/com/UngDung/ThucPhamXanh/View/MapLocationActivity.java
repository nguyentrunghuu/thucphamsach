package com.UngDung.ThucPhamXanh.View;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.UngDung.ThucPhamXanh.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapLocationActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MapLocationActivity";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker selectedMarker;
    private LatLng selectedLocation;
    private String selectedAddress = "";

    // UI Components
    private Button btnConfirmLocation, btnSearch;
    private EditText edtSearchAddress;
    ImageView btnBack;
    private FloatingActionButton fabCurrentLocation;
    private CardView cardLocationInfo;
    private TextView tvSelectedAddress, tvSelectedCoordinates;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location_simple);

        initViews();
        setupClickListeners();
        checkLocationPermission();

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            Log.d(TAG, "Map fragment found, getting map async...");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment is null! Check if fragment ID is correct.");
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        btnSearch = findViewById(R.id.btn_search);
        edtSearchAddress = findViewById(R.id.edt_search_address);
        fabCurrentLocation = findViewById(R.id.fab_current_location);
        cardLocationInfo = findViewById(R.id.card_location_info);
        tvSelectedAddress = findViewById(R.id.tv_selected_address);
        tvSelectedCoordinates = findViewById(R.id.tv_selected_coordinates);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedLocation != null && !selectedAddress.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_address", selectedAddress);
                resultIntent.putExtra("latitude", selectedLocation.latitude);
                resultIntent.putExtra("longitude", selectedLocation.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Vui lòng chọn một địa điểm trên bản đồ", Toast.LENGTH_SHORT).show();
            }
        });

        fabCurrentLocation.setOnClickListener(v -> getCurrentLocation());

        btnSearch.setOnClickListener(v -> searchAddress());

        // Set up search on Enter key press
        edtSearchAddress.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                searchAddress();
                return true;
            }
            return false;
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

        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerClickListener(marker -> {
            // Khi tap vào marker, chọn địa chỉ đó
            updateSelectedLocation(marker.getPosition());
            return true; // Consume the event
        });

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Set default location (Ho Chi Minh City)
        LatLng defaultLocation = new LatLng(10.8231, 106.6297);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));

        Log.d(TAG, "Map initialized successfully");

        // Try to get current location
        getCurrentLocation();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        selectedLocation = latLng;
        updateSelectedLocation(latLng);
    }

    private void updateSelectedLocation(LatLng latLng) {
        // Remove previous marker
        if (selectedMarker != null) {
            selectedMarker.remove();
        }

        // Add new marker
        selectedMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Địa điểm đã chọn"));

        // Show location info card
        cardLocationInfo.setVisibility(View.VISIBLE);
        tvSelectedCoordinates.setText(String.format("Tọa độ: %.6f, %.6f", latLng.latitude, latLng.longitude));

        // Get address from coordinates
        getAddressFromLocation(latLng);
    }

    private void getAddressFromLocation(LatLng latLng) {
        progressBar.setVisibility(View.VISIBLE);
        tvSelectedAddress.setText("Đang tải địa chỉ...");

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder addressBuilder = new StringBuilder();

                // Build address string
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0)
                        addressBuilder.append(", ");
                    addressBuilder.append(address.getAddressLine(i));
                }

                selectedAddress = addressBuilder.toString();
                tvSelectedAddress.setText(selectedAddress);
            } else {
                selectedAddress = String.format("Tọa độ: %.6f, %.6f", latLng.latitude, latLng.longitude);
                tvSelectedAddress.setText(selectedAddress);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting address: " + e.getMessage());
            selectedAddress = String.format("Tọa độ: %.6f, %.6f", latLng.latitude, latLng.longitude);
            tvSelectedAddress.setText(selectedAddress);
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void searchAddress() {
        String searchQuery = edtSearchAddress.getText().toString().trim();
        if (searchQuery.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ cần tìm kiếm", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvSelectedAddress.setText("Đang tìm kiếm...");

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(searchQuery, 5);
            if (addresses != null && !addresses.isEmpty()) {
                // Lấy địa chỉ đầu tiên từ kết quả tìm kiếm
                Address address = addresses.get(0);
                LatLng searchLocation = new LatLng(address.getLatitude(), address.getLongitude());

                // Di chuyển camera đến vị trí tìm được
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(searchLocation, 15));

                // Cập nhật vị trí đã chọn
                updateSelectedLocation(searchLocation);

                // Hiển thị tất cả kết quả tìm kiếm trên bản đồ
                mMap.clear();
                for (Address addr : addresses) {
                    LatLng location = new LatLng(addr.getLatitude(), addr.getLongitude());
                    String addressText = "";
                    for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                        if (i > 0)
                            addressText += ", ";
                        addressText += addr.getAddressLine(i);
                    }

                    mMap.addMarker(new MarkerOptions()
                            .position(location)
                            .title(addressText)
                            .snippet("Tap để chọn địa chỉ này"));
                }

                Toast.makeText(this, "Tìm thấy " + addresses.size() + " kết quả", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không tìm thấy địa chỉ: " + searchQuery, Toast.LENGTH_SHORT).show();
                tvSelectedAddress.setText("Không tìm thấy địa chỉ");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error searching address: " + e.getMessage());
            Toast.makeText(this, "Lỗi khi tìm kiếm địa chỉ", Toast.LENGTH_SHORT).show();
            tvSelectedAddress.setText("Lỗi khi tìm kiếm");
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        updateSelectedLocation(currentLocation);
                    } else {
                        Toast.makeText(this, "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Cần quyền truy cập vị trí để sử dụng tính năng này", Toast.LENGTH_LONG).show();
            }
        }
    }
}
