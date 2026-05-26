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

// 匯入 osmdroid 相關套件
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import com.example.newjavaproject.map.network.AqiApiClient;
import com.example.newjavaproject.R;




public class MapFragment extends Fragment {
    private MapView mMap;

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
        mMap.setMultiTouchControls(true); // 啟用兩指縮放功能

        setupMap();

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

    private void setupMap() {
        // 將原本的 LatLng 替換為 GeoPoint
        GeoPoint testLocation = new GeoPoint(23.545, 120.428);
        
        // 設定地圖視角與縮放級別
        mMap.getController().setZoom(15.0);
        mMap.getController().setCenter(testLocation);

        // 建立並加入標記 (取代原本的 MarkerOptions)
        Marker startMarker = new Marker(mMap);
        startMarker.setPosition(testLocation);
        startMarker.setTitle("氧森地圖測試成功");
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mMap.getOverlays().add(startMarker);
    }
    
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


