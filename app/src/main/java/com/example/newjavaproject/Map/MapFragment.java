package com.example.newjavaproject.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;


// 匯入 osmdroid 相關套件
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import com.example.newjavaproject.map.network.AqiApiClient;
import com.example.newjavaproject.R;




public class MapFragment extends Fragment {
    private MapView mMap;

    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if ((fineLocationGranted != null && fineLocationGranted) || 
                    (coarseLocationGranted != null && coarseLocationGranted)) {
                    // 使用者點擊了「允許」，開始抓位置
                    getCurrentLocation();
                } else {
                    // 使用者拒絕了
                    Toast.makeText(getContext(), "需要定位權限才能幫長輩找到附近的步道喔！", Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 重要：初始化 osmdroid 配置，必須在載入 layout 之前呼叫
        Configuration.getInstance().load(getContext(), androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        
        return inflater.inflate(R.layout.fragment_map, container, false);

        
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 初始化 osmdroid 地圖
        mMap = view.findViewById(R.id.mapview);
        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setMultiTouchControls(true); // 啟用兩指縮放功能

        // setupMap();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        checkLocationPermission();

        // 2. 保留你原本的 AQI API 邏輯
        TextView tvAqiValue = view.findViewById(R.id.tv_aqi_value);
        TextView tvAqiStatus = view.findViewById(R.id.tv_aqi_status);
        AqiApiClient apiClient = new AqiApiClient();
        apiClient.fetchCurrentAqi(new AqiApiClient.AqiCallback() {
            @Override
            public void onSuccess(String aqiValue, String status) {
                // TODO: 處理 AQI 成功邏輯
            }
            @Override
            public void onError(String errorMessage) {
                // TODO: 處理 AQI 失敗邏輯
            }
        });

        // 3. 保留你原本的 UI 點擊事件
        CardView cardMapPreview = view.findViewById(R.id.card_map_preview);
        cardMapPreview.setOnClickListener(v -> {
            Toast.makeText(getContext(), "[架構] 點擊了地圖預覽...", Toast.LENGTH_LONG).show();
        });

        CardView cardChartPreview = view.findViewById(R.id.card_chart_placeholder);
        cardChartPreview.setOnClickListener(v -> {
            Toast.makeText(getContext(), "[架構] 點擊了趨勢圖...", Toast.LENGTH_LONG).show();
        });
    }

    private void checkLocationPermission() {
        // 檢查是否已經有精確定位權限
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 已經有權限了，直接抓位置
            getCurrentLocation();
        } else {
            // 沒權限，跳出視窗請求權限
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    // 【步驟四】取得經緯度並移動地圖
    private void getCurrentLocation() {
        // 再次確認權限 (IDE 安全檢查要求)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // 【步驟四】取得經緯度並移動地圖
        // 使用 getCurrentLocation 主動要求最新鮮的高精度定位
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        // 成功抓到定位！取得經緯度
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();

                        // 轉換成 osmdroid 的 GeoPoint
                        GeoPoint userLocation = new GeoPoint(lat, lon);
                        
                        // 設定地圖視角與縮放級別 (16.0 看街道比較清楚)
                        mMap.getController().setZoom(16.0);
                        mMap.getController().setCenter(userLocation);

                        // 在地圖上放上一個標記代表 "現在位置"
                        Marker myMarker = new Marker(mMap);
                        myMarker.setPosition(userLocation);
                        myMarker.setTitle("您的目前位置");
                        myMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        mMap.getOverlays().add(myMarker);
                        
                        // 刷新地圖顯示
                        mMap.invalidate();

                        Toast.makeText(getContext(), "定位成功！", Toast.LENGTH_SHORT).show();
                        
                    } else {
                        Toast.makeText(getContext(), "無法取得定位，請確認手機是否開啟 GPS", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // private void setupMap() {
    //     // 將原本的 LatLng 替換為 GeoPoint
    //     GeoPoint testLocation = new GeoPoint(23.545, 120.428);
        
    //     // 設定地圖視角與縮放級別
    //     mMap.getController().setZoom(15.0);
    //     mMap.getController().setCenter(testLocation);

    //     // 建立並加入標記 (取代原本的 MarkerOptions)
    //     Marker startMarker = new Marker(mMap);
    //     startMarker.setPosition(testLocation);
    //     startMarker.setTitle("氧森地圖測試成功");
    //     startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
    //     mMap.getOverlays().add(startMarker);
    // }
    
    // osmdroid 建議加入生命週期管理以節省資源
    @Override
    public void onResume() {
        super.onResume();
        if (mMap != null) mMap.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMap != null) mMap.onPause();
    }
}


