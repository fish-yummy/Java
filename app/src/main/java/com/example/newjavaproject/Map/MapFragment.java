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

import com.example.newjavaproject.map.network.AqiApiClient;
import com.example.newjavaproject.R;





public class MapFragment extends Fragment implements OnMapReadyCallback{
    private GoogleMap mMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        TextView tvAqiValue = view.findViewById(R.id.tv_aqi_value);
        TextView tvAqiStatus = view.findViewById(R.id.tv_aqi_status);

        AqiApiClient apiClient = new AqiApiClient();
        apiClient.fetchCurrentAqi(new AqiApiClient.AqiCallback() {
            @Override
            public void onSuccess(String aqiValue, String status) {
                //在這裡將拿到的 AQI 資料更新到畫面上
                // TODO: 將 tvAqiValue 的文字設為 aqiValue
                // TODO: 將 tvAqiStatus 的文字設為 "AQI (" + status + ")"
                // TODO: (進階) 根據 aqiValue 的數值，動態改變 tvAqiValue 的顏色 (例如綠色或紅色)
            }

            @Override
            public void onError(String errorMessage) {
                // 處理 API 失敗狀態
                // TODO: 在畫面上顯示錯誤提示，或將 AQI 數值顯示為 "--"
            }
        });



        CardView cardMapPreview = view.findViewById(R.id.card_map_preview);
        cardMapPreview.setOnClickListener(v -> {
            // 這裡負責處理點擊後變成全螢幕地圖的邏輯。
        
            Toast.makeText(getContext(), "[架構] 點擊了地圖預覽，請在此啟動全螢幕地圖 Activity 或切換 Fragment。", Toast.LENGTH_LONG).show();
            
            
            
        });

        CardView cardChartPreview = view.findViewById(R.id.card_chart_placeholder);
        cardChartPreview.setOnClickListener(v -> {
            // 如果要放圖表，我原本想是一般頁面上的是預覽，點擊之後就會切換成全螢幕，怎麼切可以寫這裡
            Toast.makeText(getContext(), "[架構] 點擊了趨勢圖，在此切換至全螢幕數據圖表畫面。", Toast.LENGTH_LONG).show();
            
           
            // Intent intent = new Intent(getActivity(), FullScreenChartActivity.class);
            // startActivity(intent);
        });

    }

    

    @Override

    public void onMapReady(@NonNull GoogleMap googleMap){
        mMap = googleMap;

        // 處理綠色步道的地圖標記
        // TODO: 1. 建立周邊公園或健走路徑的座標資料 (LatLng)
        // TODO: 2. 使用 mMap.addMarker() 將這些公園標記在地圖上
        //底下5行我測試用的

        LatLng testLocation = new LatLng(23.545, 120.428);
        mMap.addMarker(new MarkerOptions()
                .position(testLocation)
                .title("氧森地圖測試成功"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(testLocation, 15f));


    }
}
