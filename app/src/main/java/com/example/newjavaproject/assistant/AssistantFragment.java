package com.example.newjavaproject.assistant;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.newjavaproject.data.SharedPrefsManager;
import com.example.newjavaproject.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AssistantFragment extends Fragment implements SensorEventListener, LocationListener {

    private Handler timeHandler;
    private Runnable timeRunnable;
    private SharedPrefsManager prefsManager;

    private Button btnStartStop;
    private Button btnResetTarget;
    private TextView tvTodayAccumulatedTitle;
    private TextView tvStatusHint;
    
    private TextView tvTimeCurrent, tvTimeTarget, tvTimePercent;
    private ProgressBar progressCircleTime;
    private TextView tvDistanceCurrent, tvDistanceTarget, tvDistancePercent;
    private ProgressBar progressCircleDistance;

    private TextView tvPoseReminderStatus;
    private TextView tvCaloriesBurnt; 
    private LinearLayout containerHistoryRecords;

    private boolean isPlaying = false;
    private long currentSessionTime = 0; 
    private double currentDistanceInKm = 0.0;
    private long targetTimeInSeconds = 0;  
    private double targetDistanceInKm = 0.0;

    // 感應器與定位相關
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastMovementTime = 0; 
    private static final int MOVEMENT_CHECK_INTERVAL_MS = 5000;

    private LocationManager locationManager;
    private Location lastLocation = null; // 用來儲存上一個 GPS 點以計算距離
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private ArrayList<String> dummyHistoryList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timeHandler = new Handler(Looper.getMainLooper());
        prefsManager = new SharedPrefsManager(requireContext());

        // 初始化感應器
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // 初始化定位管理器
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        // 註冊權限請求
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocation = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    if (fineLocation != null && fineLocation) {
                        Toast.makeText(getContext(), "定位權限已開，可以開始精準運動！", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "請允許定位權限，否則無法計算真實里程與地點。", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnStartStop = view.findViewById(R.id.btn_start_stop);
        btnResetTarget = view.findViewById(R.id.btn_reset_target);
        tvTodayAccumulatedTitle = view.findViewById(R.id.tv_today_accumulated_title);
        tvStatusHint = view.findViewById(R.id.tv_status_hint);
        tvTimeCurrent = view.findViewById(R.id.tv_time_current);
        tvTimeTarget = view.findViewById(R.id.tv_time_target);
        tvTimePercent = view.findViewById(R.id.tv_time_percent);
        progressCircleTime = view.findViewById(R.id.progress_circle_time);
        tvDistanceCurrent = view.findViewById(R.id.tv_distance_current);
        tvDistanceTarget = view.findViewById(R.id.tv_distance_target);
        tvDistancePercent = view.findViewById(R.id.tv_distance_percent);
        progressCircleDistance = view.findViewById(R.id.progress_circle_distance);
        tvPoseReminderStatus = view.findViewById(R.id.tv_pose_reminder_status);
        tvCaloriesBurnt = view.findViewById(R.id.tv_calories_burnt);
        containerHistoryRecords = view.findViewById(R.id.container_history_records);

        Button btnClearHistory = view.findViewById(R.id.btn_clear_history);
        btnClearHistory.setOnClickListener(v -> {
            dummyHistoryList.clear();
            saveHistoryToPrefs();
            updateHistoryUI();
            Toast.makeText(requireContext(), "歷史紀錄已清空", Toast.LENGTH_SHORT).show();
        });

        loadSavedProgress();
        loadHistoryFromPrefs();
        updateHistoryUI();

        // 檢查定位權限
        checkLocationPermissions();

        timeRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    currentSessionTime++;
                    
                    // 【修改】移除原本的模擬里程增加，現在完全交給真實的 LocationListener 處理
                    updateDynamicUI();
                    
                    if (System.currentTimeMillis() - lastMovementTime > MOVEMENT_CHECK_INTERVAL_MS) {
                        tvPoseReminderStatus.setText("靜止中");
                        tvPoseReminderStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark, null));
                    }

                    boolean isTimeReached = targetTimeInSeconds > 0 && currentSessionTime >= targetTimeInSeconds;
                    boolean isDistanceReached = targetDistanceInKm > 0.0 && currentDistanceInKm >= targetDistanceInKm;
                    
                    if (isTimeReached && isDistanceReached) {
                        tvStatusHint.setText("🎉 太棒了！今日運動目標全部達標！");
                        stopExerciseAndUpdateUI();
                        return;
                    }
                    
                    timeHandler.postDelayed(this, 1000);
                }
            }
        };

        btnStartStop.setOnClickListener(v -> {
            if (!isPlaying) { startExercise(); } else { stopExerciseAndUpdateUI(); }
        });

        btnResetTarget.setOnClickListener(v -> {
            if (isPlaying) {
                Toast.makeText(requireContext(), "請先停止運動，才能重新設定目標喔！", Toast.LENGTH_SHORT).show();
                return;
            }
            showTargetInputDialog();
        });

        if (targetTimeInSeconds == 0 && targetDistanceInKm == 0.0) {
            view.post(this::showTargetInputDialog);
        }
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startExercise() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "請先允許定位權限才能開始運動！", Toast.LENGTH_SHORT).show();
            return;
        }

        isPlaying = true;
        btnStartStop.setText("STOP");
        btnStartStop.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null));
        btnResetTarget.setVisibility(View.GONE);
        tvStatusHint.setText("真實定位追蹤中...請開始慢跑或健走");
        lastMovementTime = System.currentTimeMillis();
        tvPoseReminderStatus.setText("穩定維持");
        tvPoseReminderStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
        
        // 重設 GPS 的起始點
        lastLocation = null;

        // 啟動感應器
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // 啟動真實 GPS 監聽：每 2 秒或移動超過 1 公尺就更新
        if (locationManager != null) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1.0f, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 1.0f, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        timeHandler.post(timeRunnable);
    }

    private void stopExerciseAndUpdateUI() {
        isPlaying = false;
        timeHandler.removeCallbacksAndMessages(null); 
        btnStartStop.setText("START");
        btnStartStop.setBackgroundColor(0xff2e7d32); 
        btnResetTarget.setVisibility(View.VISIBLE);
        tvPoseReminderStatus.setText("未啟動");
        tvPoseReminderStatus.setTextColor(0xff4caf50);

        // 註銷感應器與定位監聽
        if (sensorManager != null) { sensorManager.unregisterListener(this); }
        if (locationManager != null) { locationManager.removeUpdates(this); }

        if (currentSessionTime > 5) {
            double calories = currentDistanceInKm * 60.0;
            
            // 【核心優化】獲取最後定位的真實地址
            String locationAddress = getAddressFromLocation(lastLocation);

            String newRecord = String.format(Locale.getDefault(), "🔥 %s | 時間: %02d:%02d | 里程: %.2f km | 消耗: %.1f kcal",
                    locationAddress, currentSessionTime / 60, currentSessionTime % 60, currentDistanceInKm, calories);
            
            dummyHistoryList.add(0, newRecord);
            if (dummyHistoryList.size() > 10) { dummyHistoryList.remove(10); }
            saveHistoryToPrefs();
            updateHistoryUI();
        }
        saveCurrentProgress();
        if (currentSessionTime > 0) {
            try { prefsManager.addExerciseTime(currentSessionTime); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 【新增】利用 Geocoder 反查經緯度成真實地址的函式
    private String getAddressFromLocation(Location location) {
        if (location == null) return "未知地點";
        
        // 建立 Geocoder
        Geocoder geocoder = new Geocoder(requireContext(), Locale.TAIWAN);
        try {
            // 透過經緯度抓取一筆最接近的地址資訊
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                
                // 在 Android 台灣地址中，縣市名稱（如：嘉義市、台南市）通常儲存在 getAdminArea()
                String city = address.getAdminArea() != null ? address.getAdminArea() : "";
                
                // 取得路段/街道 (例如：新民路、中山路)
                String street = address.getThoroughfare() != null ? address.getThoroughfare() : "";
                
                // 如果沒有路名（例如在鄉間），就拿鄉鎮市區（getLocality）湊著用
                if (street.isEmpty() && address.getLocality() != null) {
                    street = address.getLocality();
                }

                if (!city.isEmpty() || !street.isEmpty()) {
                    return city + street;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "未知地點";
    }

    // --- LocationListener 介面實作（即時 GPS 數據進來的地方） ---
    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (!isPlaying) return;

        // 如果是第一個定位點，先記錄下來，不計算距離
        if (lastLocation == null) {
            lastLocation = location;
            return;
        }

        // 計算上個點到這個點的距離（單位是公尺）
        float distanceInMeters = lastLocation.distanceTo(location);

        // 過濾掉 GPS 跳躍的雜訊（例如：時速超過 40 公里可能是不合理的走路數據，或者是定位飄移）
        if (distanceInMeters > 0.5 && distanceInMeters < 30.0) {
            // 將公尺轉換成公里，累加至當前里程數
            currentDistanceInKm += (distanceInMeters / 1000.0);
            
            // 更新最後的位置點
            lastLocation = location;

            // 即時刷新介面上的里程數字與進度條
            updateDynamicUI();
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}


    // --- SensorEventListener 實作保持不變 ---
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0]; float y = event.values[1]; float z = event.values[2];
            double acceleration = Math.sqrt(x * x + y * y + z * z);
            if (Math.abs(acceleration - SensorManager.GRAVITY_EARTH) > 1.5) {
                lastMovementTime = System.currentTimeMillis();
                if (isPlaying) {
                    tvPoseReminderStatus.setText("穩定維持");
                    tvPoseReminderStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void saveHistoryToPrefs() {
        if (getContext() == null) return;
        android.content.SharedPreferences sp = requireContext().getSharedPreferences("RunningPrefs", Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (String s : dummyHistoryList) { sb.append(s).append("|||"); }
        sp.edit().putString("history_records", sb.toString()).apply();
    }

    private void loadHistoryFromPrefs() {
        if (getContext() == null) return;
        android.content.SharedPreferences sp = requireContext().getSharedPreferences("RunningPrefs", Context.MODE_PRIVATE);
        String savedRecords = sp.getString("history_records", "");
        dummyHistoryList.clear(); 
        if (savedRecords != null && !savedRecords.isEmpty()) {
            String[] parts = savedRecords.split("\\|\\|\\|");
            for (String s : parts) { if (!s.isEmpty()) { dummyHistoryList.add(s); } }
        }
    }

    private void updateHistoryUI() {
        if (containerHistoryRecords == null || getContext() == null) return;
        containerHistoryRecords.removeAllViews();
        for (String record : dummyHistoryList) {
            TextView itemText = new TextView(requireContext());
            itemText.setText(record);
            itemText.setTextSize(13);
            itemText.setTextColor(0xff555555);
            itemText.setPadding(4, 12, 4, 12);
            View divider = new View(requireContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
            divider.setBackgroundColor(0xffe0e0e0);
            containerHistoryRecords.addView(itemText);
            containerHistoryRecords.addView(divider);
        }
    }

    private void loadSavedProgress() {
        if (getContext() == null) return;
        android.content.SharedPreferences sp = requireContext().getSharedPreferences("RunningPrefs", android.content.Context.MODE_PRIVATE);
        targetTimeInSeconds = sp.getLong("target_time", 0);
        targetDistanceInKm = Double.longBitsToDouble(sp.getLong("target_distance", Double.doubleToLongBits(0.0)));
        currentSessionTime = sp.getLong("current_time", 0);
        currentDistanceInKm = Double.longBitsToDouble(sp.getLong("current_real_distance", Double.doubleToLongBits(0.0)));
        if (targetTimeInSeconds > 0 || targetDistanceInKm > 0.0) {
            long minutes = targetTimeInSeconds / 60;
            tvTodayAccumulatedTitle.setText("今日目標：" + minutes + " 分鐘");
            tvStatusHint.setText("歡迎回來，隨時可以點擊開始！");
            updateDynamicUI();
        }
    }

    private void saveCurrentProgress() {
        if (getContext() == null) return;
        android.content.SharedPreferences sp = requireContext().getSharedPreferences("RunningPrefs", android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sp.edit();
        editor.putLong("target_time", targetTimeInSeconds);
        editor.putLong("target_distance", Double.doubleToLongBits(targetDistanceInKm));
        editor.putLong("current_time", currentSessionTime);
        editor.putLong("current_real_distance", Double.doubleToLongBits(currentDistanceInKm));
        editor.apply();
    }

    private void updateDynamicUI() {
        long currentMin = currentSessionTime / 60;
        long currentSec = currentSessionTime % 60;
        tvTimeCurrent.setText(String.format(Locale.getDefault(), "%02d:%02d", currentMin, currentSec));
        tvTimeTarget.setText(String.format(Locale.getDefault(), " / %02d:00", targetTimeInSeconds / 60));
        int timePercent = targetTimeInSeconds > 0 ? (int) (((double) currentSessionTime / targetTimeInSeconds) * 100) : 0;
        if (timePercent > 100) timePercent = 100;
        tvTimePercent.setText(timePercent + "%");
        progressCircleTime.setProgress(timePercent);
        
        tvDistanceCurrent.setText(String.format(Locale.getDefault(), "%.2f", currentDistanceInKm));
        tvDistanceTarget.setText(String.format(Locale.getDefault(), " / %.2f km", targetDistanceInKm));
        int distPercent = targetDistanceInKm > 0.0 ? (int) ((currentDistanceInKm / targetDistanceInKm) * 100) : 0;
        if (distPercent > 100) distPercent = 100;
        tvDistancePercent.setText(distPercent + "%");
        progressCircleDistance.setProgress(distPercent);
        
        double calories = currentDistanceInKm * 60.0;
        tvCaloriesBurnt.setText(String.format(Locale.getDefault(), "%.1f kcal", calories));
        saveCurrentProgress();
    }

    private void showTargetInputDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_target_input, null);
        EditText etMinutes = dialogView.findViewById(R.id.et_target_minutes);
        EditText etDistance = dialogView.findViewById(R.id.et_target_distance);
        if (targetTimeInSeconds > 0) etMinutes.setText(String.valueOf(targetTimeInSeconds / 60));
        if (targetDistanceInKm > 0.0) etDistance.setText(String.valueOf(targetDistanceInKm));
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("今日運動目標設定")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("確認設定", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String minStr = etMinutes.getText().toString().trim();
                String distStr = etDistance.getText().toString().trim();
                if (minStr.isEmpty() || distStr.isEmpty()) {
                    Toast.makeText(requireContext(), "請輸入預計時長與里程數", Toast.LENGTH_SHORT).show();
                    return;
                }
                long minutes = Long.parseLong(minStr);
                targetTimeInSeconds = minutes * 60;
                targetDistanceInKm = Double.parseDouble(distStr);
                currentSessionTime = 0;
                currentDistanceInKm = 0.0;
                tvTodayAccumulatedTitle.setText("今日目標：" + minutes + " 分鐘");
                tvStatusHint.setText("新目標設定成功，準備起跑！");
                updateDynamicUI();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 移除原先在 onPause 自動停止運動的邏輯，讓使用者在切換頁面時後台仍能保持計時與真實 GPS 定位。
    }
}