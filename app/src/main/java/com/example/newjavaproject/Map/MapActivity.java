package com.example.newjavaproject.map;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

// 匯入 Google Maps 相關套件
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.example.newjavaproject.R;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_map);

      
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng testLocation = new LatLng(23.545, 120.428);

        mMap.addMarker(new MarkerOptions()
                .position(testLocation)
                .title("氧森地圖測試成功！"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(testLocation, 15f));
    }
}
