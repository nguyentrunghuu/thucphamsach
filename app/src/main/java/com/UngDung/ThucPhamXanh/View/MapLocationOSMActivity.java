package com.UngDung.ThucPhamXanh.View;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapLocationOSMActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MapLocationOSMActivity";

    private MapView mapView;
    private IMapController mapController;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker selectedMarker;
    private GeoPoint selectedLocation;
    private String selectedAddress = "";
    private boolean hasExistingAddress = false;

    // UI Components
    private Button btnConfirmLocation, btnSearch;
    private EditText edtSearchAddress;
    private ImageView btnBack;
    private FloatingActionButton fabCurrentLocation;
    private CardView cardLocationInfo;
    private TextView tvSelectedAddress, tvSelectedCoordinates;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location_osm);

        // Cấu hình OpenStreetMap
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));

        // Kiểm tra xem có địa chỉ được truyền vào không
        checkForExistingAddress();

        initViews();
        setupClickListeners();
        checkLocationPermission();
        initMap();

        // Hiển thị địa chỉ hiện tại nếu có
        if (hasExistingAddress) {
            tvSelectedAddress.setText(selectedAddress);
            cardLocationInfo.setVisibility(View.VISIBLE);
            btnConfirmLocation.setText("Xác nhận địa chỉ hiện tại");
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void checkForExistingAddress() {
        // Kiểm tra xem có địa chỉ được truyền vào từ Intent không
        String existingAddress = getIntent().getStringExtra("existing_address");
        if (existingAddress != null && !existingAddress.isEmpty()) {
            hasExistingAddress = true;
            selectedAddress = existingAddress;
            Log.d(TAG, "Có địa chỉ hiện tại: " + existingAddress);
        } else {
            hasExistingAddress = false;
            Log.d(TAG, "Không có địa chỉ hiện tại, bắt người dùng chọn");
        }
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
        mapView = findViewById(R.id.map_view);
    }

    private void initMap() {
        // Cấu hình map
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(15);

        // Set default location (Ho Chi Minh City)
        GeoPoint defaultLocation = new GeoPoint(10.8231, 106.6297);
        mapController = mapView.getController();
        mapController.setCenter(defaultLocation);

        // Thêm click listener cho map
        mapView.getOverlays().add(new Overlay() {
            @Override
            public boolean onSingleTapUp(MotionEvent e, MapView mapView) {
                if (e != null) {
                    org.osmdroid.util.GeoPoint geoPoint = (org.osmdroid.util.GeoPoint) mapView.getProjection()
                            .fromPixels((int) e.getX(), (int) e.getY());
                    selectedLocation = geoPoint;
                    updateSelectedLocation(geoPoint);
                    return true;
                }
                return false;
            }
        });

        Log.d(TAG, "Map initialized successfully");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnConfirmLocation.setOnClickListener(v -> {
            // Nếu đã có địa chỉ từ server, cho phép xác nhận ngay
            if (hasExistingAddress && !selectedAddress.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_address", selectedAddress);
                // Nếu có vị trí được chọn, sử dụng vị trí đó, nếu không sử dụng vị trí mặc định
                if (selectedLocation != null) {
                    resultIntent.putExtra("latitude", selectedLocation.getLatitude());
                    resultIntent.putExtra("longitude", selectedLocation.getLongitude());
                } else {
                    // Sử dụng vị trí mặc định (Hà Nội)
                    resultIntent.putExtra("latitude", 21.0285);
                    resultIntent.putExtra("longitude", 105.8542);
                }
                setResult(RESULT_OK, resultIntent);
                finish();
            } else if (selectedLocation != null && !selectedAddress.isEmpty()) {
                // Nếu người dùng đã chọn địa điểm mới
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_address", selectedAddress);
                resultIntent.putExtra("latitude", selectedLocation.getLatitude());
                resultIntent.putExtra("longitude", selectedLocation.getLongitude());
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

    private void updateSelectedLocation(GeoPoint geoPoint) {
        selectedLocation = geoPoint;

        // Remove previous marker
        if (selectedMarker != null) {
            mapView.getOverlays().remove(selectedMarker);
        }

        // Add new marker
        selectedMarker = new Marker(mapView);
        selectedMarker.setPosition(geoPoint);
        selectedMarker.setTitle("Địa điểm đã chọn");
        mapView.getOverlays().add(selectedMarker);

        // Show location info card
        cardLocationInfo.setVisibility(View.VISIBLE);
        tvSelectedCoordinates.setText(String.format("Tọa độ: %.6f, %.6f",
                geoPoint.getLatitude(), geoPoint.getLongitude()));

        // Cập nhật text nút xác nhận khi người dùng chọn địa chỉ mới
        btnConfirmLocation.setText("Xác nhận địa chỉ mới");

        // Get address from coordinates
        getAddressFromLocation(geoPoint);

        // Refresh map
        mapView.invalidate();
    }

    private void getAddressFromLocation(GeoPoint geoPoint) {
        progressBar.setVisibility(View.VISIBLE);
        tvSelectedAddress.setText("Đang tải địa chỉ...");

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
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
                selectedAddress = String.format("Tọa độ: %.6f, %.6f",
                        geoPoint.getLatitude(), geoPoint.getLongitude());
                tvSelectedAddress.setText(selectedAddress);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting address: " + e.getMessage());
            selectedAddress = String.format("Tọa độ: %.6f, %.6f",
                    geoPoint.getLatitude(), geoPoint.getLongitude());
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
                GeoPoint searchLocation = new GeoPoint(address.getLatitude(), address.getLongitude());

                // Di chuyển camera đến vị trí tìm được
                mapController.animateTo(searchLocation);

                // Cập nhật vị trí đã chọn
                updateSelectedLocation(searchLocation);

                // Hiển thị tất cả kết quả tìm kiếm trên bản đồ
                mapView.getOverlays().clear();
                for (Address addr : addresses) {
                    GeoPoint location = new GeoPoint(addr.getLatitude(), addr.getLongitude());
                    String addressText = "";
                    for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                        if (i > 0)
                            addressText += ", ";
                        addressText += addr.getAddressLine(i);
                    }

                    Marker marker = new Marker(mapView);
                    marker.setPosition(location);
                    marker.setTitle(addressText);
                    marker.setSnippet("Tap để chọn địa chỉ này");
                    mapView.getOverlays().add(marker);
                }

                // Thêm lại click listener
                mapView.getOverlays().add(new Overlay() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e, MapView mapView) {
                        if (e != null) {
                            org.osmdroid.util.GeoPoint geoPoint = (org.osmdroid.util.GeoPoint) mapView.getProjection()
                                    .fromPixels((int) e.getX(), (int) e.getY());
                            selectedLocation = geoPoint;
                            updateSelectedLocation(geoPoint);
                            return true;
                        }
                        return false;
                    }
                });

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
                        GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mapController.animateTo(currentLocation);
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

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
