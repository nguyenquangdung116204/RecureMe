package com.example.testmap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapFragment extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_CODE = 100;
    private SearchView searchView;
    private LatLng currentLocation;
    private LatLng destinationLocation;
    private RequestQueue requestQueue;
    private static final String API_KEY = "AIzaSyBLDg-lzYFM-GviLe_TWX2doXfOf0xfgpM";
    private DatabaseReference databaseReference;
    private MaterialButton bookServiceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_map);

        databaseReference = FirebaseDatabase.getInstance().getReference("current_location");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        bookServiceButton = findViewById(R.id.book_service_button);
        bookServiceButton.setOnClickListener(v -> {
            Intent intent = new Intent(MapFragment.this, ServiceSelectionActivity.class);
            startActivity(intent);
        });

        searchView = findViewById(R.id.search_view);
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Tìm kiếm");
        searchView.setFocusable(true);
        searchView.setFocusableInTouchMode(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        setupSearchView();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (googleMap != null) {
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d("MapFragment", "Vị trí hiện tại: " + currentLocation.latitude + ", " + currentLocation.longitude);
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        googleMap.addMarker(new MarkerOptions().position(currentLocation).title("Vị trí hiện tại"));
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                        saveCurrentLocationToFirebase(currentLocation);
                    }
                }
            }
        };

        checkLocationPermission();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Cần quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE
            );
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (googleMap != null) {
                startLocationUpdates();
            }
        } else {
            Toast.makeText(this, "Quyền truy cập vị trí bị từ chối", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setOnClickListener(v -> {
            searchView.setIconified(false);
            searchView.requestFocus();
        });
    }

    private void searchLocation(String locationName) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                destinationLocation = new LatLng(address.getLatitude(), address.getLongitude());
                Log.d("MapFragment", "Destination: " + destinationLocation.latitude + ", " + destinationLocation.longitude);

                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(destinationLocation).title(locationName));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destinationLocation, 15));

                if (currentLocation != null) {
                    Log.d("MapFragment", "Current: " + currentLocation.latitude + ", " + currentLocation.longitude);
                    googleMap.addMarker(new MarkerOptions().position(currentLocation).title("Vị trí hiện tại"));
                    drawRoute(currentLocation, destinationLocation);
                } else {
                    Toast.makeText(this, "Chưa có vị trí hiện tại", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Không tìm thấy địa điểm: " + locationName, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi tìm kiếm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCurrentLocationToFirebase(LatLng location) {
        DatabaseReference ref = databaseReference;
        ref.child("latitude").setValue(location.latitude);
        ref.child("longitude").setValue(location.longitude);
        ref.child("timestamp").setValue(System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã lưu vị trí hiện tại lên Firebase", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi lưu vị trí: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Không có kết nối internet", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=" + API_KEY;
        Log.d("MapFragment", "URL Directions API: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String status = response.getString("status");
                        Log.d("MapFragment", "API Status: " + status);

                        if (status.equals("OK")) {
                            JSONArray routes = response.getJSONArray("routes");
                            if (routes.length() > 0) {
                                JSONObject route = routes.getJSONObject(0);
                                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                                String points = overviewPolyline.getString("points");

                                List<LatLng> decodedPath = decodePoly(points);
                                googleMap.addPolyline(new PolylineOptions()
                                        .addAll(decodedPath)
                                        .width(10)
                                        .color(Color.BLUE));
                            } else {
                                Toast.makeText(this, "Không tìm thấy đường đi giữa hai điểm", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Lỗi API: " + status, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Lỗi xử lý dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Lỗi kết nối API: " + error.toString(), Toast.LENGTH_SHORT).show();
                    Log.e("MapFragment", "Volley Error: " + error.toString());
                }
        );

        requestQueue.add(jsonObjectRequest);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }
        return poly;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}