package com.example.newjavaproject.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

// 匯入 osmdroid 相關套件
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.example.newjavaproject.map.network.AqiApiClient;
import com.example.newjavaproject.R;

public class MapFragment extends Fragment {
    private MapView mMap;
    private MyLocationNewOverlay myLocationOverlay; // 宣告真實定位圖層
    private ActivityResultLauncher<String[]> locationPermissionRequest; // 宣告權限請求發射器

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 註冊權限請求的回呼 (必須在 Fragment 建立時初始化)
        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    
                    if (fineLocationGranted != null && fineLocationGranted) {
                        // 獲得精確定位權限
                        enableRealLocation();
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        // 獲得大略定位權限
                        enableRealLocation();
                    } else {
                        // 使用者拒絕了權限
                        Toast.makeText(getContext(), "需要定位權限才能顯示您的目前位置喔！", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

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

        // 1. 初始化 osmdroid 地圖基本設定
        mMap = view.findViewById(R.id.mapview);
        mMap.setMultiTouchControls(true); // 啟用兩指縮放功能
        mMap.getController().setZoom(18.0); // 設定較近的預設縮放級別

        // 檢查並要求定位權限
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

    // 檢查權限的獨立方法
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 如果已經有權限，直接啟動定位
            enableRealLocation();
        } else {
            // 如果沒有權限，跳出系統對話框詢問使用者
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    // 啟動真實定位的核心邏輯
    private void enableRealLocation() {
        if (mMap == null) return;

        // 建立真實定位的圖層
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mMap);
        
        // 啟用定位
        myLocationOverlay.enableMyLocation();
        
        // 讓地圖視角自動跟著使用者移動（可選，如果你不想要一開始就鎖定視角，可以把這行註解掉）
        myLocationOverlay.enableFollowLocation();

        // 將定位圖層加入地圖中
        mMap.getOverlays().add(myLocationOverlay);
    }
    
    // 生命週期管理 (為真實定位追加了啟用與關閉，節省手機電量)
    @Override
    public void onResume() {
        super.onResume();
        if (mMap != null) mMap.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMap != null) mMap.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation(); // 退到背景時停止抓GPS
    }
}