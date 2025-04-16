package com.example.testmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RescureFindingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_CODE = 102;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng currentLocation;
    private LinearLayout llNotification;
    private LinearLayout llServiceName;
    private TextView tvServiceName;
    private Button btnCancel;
    private String serviceName;
    private String description;
    private String mediaInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finding_service);

        // Ẩn ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Khởi tạo FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Ánh xạ các thành phần giao diện
        llNotification = findViewById(R.id.ll_notification);
        llServiceName = findViewById(R.id.ll_service_name);
        tvServiceName = findViewById(R.id.tv_service_name);
        btnCancel = findViewById(R.id.btn_cancel);

        // Nhận dữ liệu từ Intent
        serviceName = getIntent().getStringExtra("SERVICE_NAME");
        description = getIntent().getStringExtra("DESCRIPTION");
        mediaInfo = getIntent().getStringExtra("MEDIA_INFO");

        // Hiển thị tên dịch vụ
        if (serviceName != null) {
            tvServiceName.setText(serviceName);
        }

        // Khởi tạo bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Xử lý sự kiện nhấn vào LinearLayout chứa "TÊN DỊCH VỤ"
        llServiceName.setOnClickListener(v -> {
            Toast.makeText(this, "Mở chi tiết dịch vụ: " + tvServiceName.getText(), Toast.LENGTH_SHORT).show();
            // Bạn có thể chuyển sang Activity chi tiết dịch vụ tại đây
        });

        // Xử lý sự kiện nhấn vào nút "HỦY"
        btnCancel.setOnClickListener(v -> {
            Toast.makeText(this, "Đã hủy booking", Toast.LENGTH_SHORT).show();
            finish(); // Quay lại ServiceDetailActivity
        });

        // Thiết lập LocationCallback để lấy vị trí người dùng
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (mMap != null) {
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(currentLocation).title("Vị trí của bạn"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
                        fusedLocationClient.removeLocationUpdates(locationCallback);

                        // Hiển thị các gara gần vị trí hiện tại
                        showNearbyGarages();
                    }
                }
            }
        };

        // Kiểm tra quyền truy cập vị trí
        checkLocationPermission();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Kích hoạt nút "My Location" trên bản đồ
        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Cần quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
        }
    }

    // Kiểm tra quyền truy cập vị trí
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Yêu cầu quyền truy cập vị trí
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_CODE);
    }

    // Xử lý kết quả yêu cầu quyền
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkLocationPermission()) {
                    mMap.setMyLocationEnabled(true);
                    startLocationUpdates();
                }
            } else {
                Toast.makeText(this, "Quyền truy cập vị trí bị từ chối!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Bắt đầu cập nhật vị trí
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    // Hiển thị các gara gần vị trí hiện tại
    private void showNearbyGarages() {
        // Lấy danh sách các gara
        List<Garage> nearbyGarages = getNearbyGarages();

        // Sắp xếp các gara theo khoảng cách
        Collections.sort(nearbyGarages, new Comparator<Garage>() {
            @Override
            public int compare(Garage g1, Garage g2) {
                float[] distance1 = new float[1];
                float[] distance2 = new float[1];
                Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, g1.latitude, g1.longitude, distance1);
                Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, g2.latitude, g2.longitude, distance2);
                return Float.compare(distance1[0], distance2[0]);
            }
        });

        // Giới hạn số lượng gara hiển thị (tối đa 3)
        int maxGaragesToShow = Math.min(nearbyGarages.size(), 3);

        // Thêm marker cho các gara gần nhất
        for (int i = 0; i < maxGaragesToShow; i++) {
            Garage garage = nearbyGarages.get(i);
            LatLng garageLocation = new LatLng(garage.latitude, garage.longitude);
            mMap.addMarker(new MarkerOptions()
                    .position(garageLocation)
                    .title(garage.name));
        }

        // Thông báo nếu có nhiều hơn 3 gara
        if (nearbyGarages.size() > 3) {
            Toast.makeText(this, "Có " + nearbyGarages.size() + " gara gần bạn, chỉ hiển thị 3 gara gần nhất.", Toast.LENGTH_LONG).show();
        }
    }

    // Lớp để lưu thông tin gara
    private static class Garage {
        String name;
        double latitude;
        double longitude;

        Garage(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    // Giả lập danh sách các gara gần vị trí hiện tại
    private List<Garage> getNearbyGarages() {
        List<Garage> garages = new ArrayList<>();
        if (currentLocation != null) {
            // Giả lập 5 gara để kiểm tra trường hợp nhiều hơn 3
            garages.add(new Garage("Gara 1", currentLocation.latitude + 0.005, currentLocation.longitude + 0.005)); // ~500m
            garages.add(new Garage("Gara 2", currentLocation.latitude - 0.003, currentLocation.longitude - 0.003)); // ~300m
            garages.add(new Garage("Gara 3", currentLocation.latitude + 0.008, currentLocation.longitude - 0.002)); // ~800m
            garages.add(new Garage("Gara 4", currentLocation.latitude - 0.006, currentLocation.longitude + 0.004)); // ~600m
            garages.add(new Garage("Gara 5", currentLocation.latitude + 0.002, currentLocation.longitude + 0.001)); // ~200m
        }
        return garages;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}