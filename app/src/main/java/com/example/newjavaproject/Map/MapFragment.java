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
import android.widget.LinearLayout;

import java.util.List;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;



// 匯入 osmdroid 相關套件
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import com.example.newjavaproject.map.network.AqiApiClient;
import com.example.newjavaproject.map.network.OverpassApiClient;
import com.example.newjavaproject.BuildConfig;

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
                    Toast.makeText(getContext(), "請開啟定位權限", Toast.LENGTH_LONG).show();
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


        // 3. 保留你原本的 UI 點擊事件
        CardView cardMapPreview = view.findViewById(R.id.card_map_preview);
        cardMapPreview.setOnClickListener(v -> {
            Toast.makeText(getContext(), "[架構] 點擊了地圖預覽...", Toast.LENGTH_LONG).show();
        });

        // CardView cardChartPreview = view.findViewById(R.id.card_chart_placeholder);
        // cardChartPreview.setOnClickListener(v -> {
        //     Toast.makeText(getContext(), "[架構] 點擊了趨勢圖...", Toast.LENGTH_LONG).show();
        // });
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

                        Toast.makeText(getContext(), "定位成功", Toast.LENGTH_SHORT).show();

                        // 【修改邏輯】1. 利用 Geocoder 將經緯度轉成縣市名稱
                        new Thread(() -> {
                            try {
                                // 【關鍵修改 1】強制要求 Geocoder 回傳繁體中文 (台灣)，避免模擬器英文語系干擾
                                android.location.Geocoder geocoder = new android.location.Geocoder(requireContext(), java.util.Locale.TAIWAN);
                                java.util.List<android.location.Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                                
                                if (addresses != null && !addresses.isEmpty()) {
                                    android.location.Address address = addresses.get(0);
                                    
                                    // 嘗試取得行政區
                                    String adminArea = address.getAdminArea(); 
                                    
                                    // 【關鍵修改 2】如果 AdminArea 是空的，在台灣有時候縣市會跑到 SubAdminArea 去
                                    if (adminArea == null) {
                                        adminArea = address.getSubAdminArea();
                                    }

                                    // 【除錯用】把抓到的完整地址和縣市印在 Logcat，我們來看它到底回傳了什麼
                                    android.util.Log.d("AQI_DEBUG", "完整地址: " + address.toString());
                                    android.util.Log.d("AQI_DEBUG", "最終決定使用的縣市字串: " + adminArea);
                                    
                                    if (adminArea != null) {
                                        // 統一替換「台」為「臺」以符合政府資料庫
                                        final String targetCounty = adminArea.replace("台", "臺");

                                        requireActivity().runOnUiThread(() -> {
                                            // 把目標縣市也用 Toast 印出來確認
                                            Toast.makeText(getContext(), "準備查詢: " + targetCounty, Toast.LENGTH_SHORT).show();
                                            fetchLocalAirQuality(targetCounty);
                                        });
                                    } else {
                                        requireActivity().runOnUiThread(() -> 
                                            Toast.makeText(getContext(), "無法從座標解析出縣市名稱", Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                }
                            } catch (java.io.IOException e) {
                                e.printStackTrace();
                                requireActivity().runOnUiThread(() -> 
                                    Toast.makeText(getContext(), "取得縣市名稱發生網路錯誤", Toast.LENGTH_SHORT).show()
                                );
                            }
                        }).start();

                        //定位和地圖設定完成後，開始呼叫 Overpass API 抓附近的公園和學校
                        OverpassApiClient overpassClient = new OverpassApiClient();
                        overpassClient.fetchNearbyParks(lat, lon, new OverpassApiClient.OverpassCallback() {
                            @Override
                            public void onSuccess(List<OverpassApiClient.ParkLocation> locations) {
                                // 避免 Fragment 已經關閉但 API 剛好回來導致閃退
                                if (getActivity() == null || getView() == null) return;
                                
                                requireActivity().runOnUiThread(() -> {
                                    // 【步驟1】抓到容器並清空舊的假資料
                                    LinearLayout layoutWalkingPaths = getView().findViewById(R.id.layout_walking_paths);
                                    layoutWalkingPaths.removeAllViews();

                                    if (locations.isEmpty()) {
                                        Toast.makeText(getContext(), "附近 1 公里內沒有找到適合健走點位", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    // 取得手機螢幕密度 (讓 Java 動態生成的 UI 間距跟 XML 一樣好看)
                                    float density = getResources().getDisplayMetrics().density;
                                    int marginPx = (int) (8 * density);
                                    int paddingPx = (int) (16 * density);

                                    // 跑迴圈，處理每一個找到的公園
                                    for (OverpassApiClient.ParkLocation park : locations) {
                                        // -- 地圖標記邏輯 --
                                        Marker parkMarker = new Marker(mMap);
                                        GeoPoint parkPoint = new GeoPoint(park.lat, park.lon);
                                        parkMarker.setPosition(parkPoint);
                                        parkMarker.setTitle(park.name);
                                        parkMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                        mMap.getOverlays().add(parkMarker);

                                        // -- 【步驟3】計算距離邏輯 --
                                        float[] results = new float[1];
                                        // 計算「長輩位置(lat, lon)」與「公園位置(park.lat, park.lon)」的距離，結果會存進 results[0]
                                        Location.distanceBetween(lat, lon, park.lat, park.lon, results);
                                        float distanceInMeters = results[0];
                                        
                                        // 判斷要顯示公尺(m)還是公里(km)，超過 1000 公尺就轉換
                                        String distanceText;
                                        if (distanceInMeters >= 1000) {
                                            distanceText = String.format("%.1f km", distanceInMeters / 1000f);
                                        } else {
                                            distanceText = String.format("%d m", (int) distanceInMeters);
                                        }

                                        // -- 【步驟2】動態生成卡片邏輯 --
                                        CardView cardView = new CardView(requireContext());
                                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        );
                                        cardParams.setMargins(0, 0, 0, marginPx); // 設定底部間距
                                        cardView.setLayoutParams(cardParams);
                                        cardView.setRadius(8 * density); // 圓角
                                        cardView.setCardElevation(1 * density); // 陰影

                                        android.util.TypedValue outValue = new android.util.TypedValue();
                                        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                                        cardView.setForeground(androidx.core.content.ContextCompat.getDrawable(requireContext(), outValue.resourceId));
                                        cardView.setClickable(true);
                                        cardView.setFocusable(true);

                                        TextView textView = new TextView(requireContext());
                                        textView.setLayoutParams(new ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        ));
                                        textView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                                        // 將名稱與計算好的距離組合起來
                                        textView.setText("📍 " + park.name + " (" + distanceText + ")");
                                        textView.setTextColor(android.graphics.Color.parseColor("#444444"));
                                        textView.setTextSize(14f);

                                        // 把文字塞進卡片裡
                                        cardView.addView(textView);

                                        // -- 【步驟4】設定點擊事件與地圖連動 --
                                        cardView.setOnClickListener(v -> {
                                            // 地圖平滑移動到該公園的座標
                                            mMap.getController().animateTo(parkPoint);
                                            // 稍微放大視角，讓長輩看清楚附近街道
                                            mMap.getController().setZoom(17.5);
                                            // 自動彈出地標的名稱泡泡
                                            parkMarker.showInfoWindow();
                                        });

                                        // 最後，將這張完整的卡片塞進底部的 LinearLayout 容器裡
                                        layoutWalkingPaths.addView(cardView);
                                    }

                                    // 全部加完之後刷新地圖
                                    mMap.invalidate();
                                    // Toast.makeText(getContext(), "已找到" + locations.size() + " 個健走點位", Toast.LENGTH_LONG).show();
                                });
                            }

                            @Override
                            public void onError(String errorMessage) {
                                if (getActivity() == null) return;
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "尋找點位時發生網路錯誤", Toast.LENGTH_SHORT).show();
                                });
                            }
                        });


                        
                    } else {
                        Toast.makeText(getContext(), "無法取得定位，請確認手機是否開啟 GPS", Toast.LENGTH_LONG).show();
                    }
                });
    }



    private void fetchLocalAirQuality(String county) {
        AqiApiClient apiClient = new AqiApiClient();
        String myApiKey = BuildConfig.PM25_API_KEY;

        apiClient.fetchAveragePm25(county, myApiKey, new AqiApiClient.AqiCallback() {
            @Override
            public void onSuccess(String pm25Value, String status, String colorHex, String countyName) {
                if (getActivity() == null || getView() == null) return;
                
                requireActivity().runOnUiThread(() -> {
                    TextView tvAqiValue = getView().findViewById(R.id.tv_aqi_value);
                    TextView tvAqiStatus = getView().findViewById(R.id.tv_aqi_status);
                    
                    // 抓取剛剛新增的儀表板 ProgressBar
                    android.widget.ProgressBar progressAqiValue = getView().findViewById(R.id.progress_aqi_value);

                    // 1. 更新數值文字與顏色
                    tvAqiValue.setText(pm25Value);
                    tvAqiValue.setTextColor(android.graphics.Color.parseColor(colorHex));
                    
                    // 2. 更新狀態文字
                    tvAqiStatus.setText(countyName + " PM2.5 (" + status + ")");

                    // 3. 動態設定儀表板的進度與顏色
                    try {
                        int pm25Int = Integer.parseInt(pm25Value);
                        // 將 PM2.5 數值映射到 0~100 的進度條，你可以調整這個比例
                        // 假設 PM2.5 = 75 就滿格 (危害等級)，為了視覺效果，我們設定最大值為 100
                        int progressValue = Math.min((pm25Int * 100) / 75, 100); 
                        
                        // 設定進度 (加上一點動畫效果)
                        progressAqiValue.setProgress(progressValue, true); 

                        // 動態改變儀表板的顏色
                        progressAqiValue.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(colorHex))
                        );
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "空品資料載入失敗", Toast.LENGTH_SHORT).show()
                );
            }
        });
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


