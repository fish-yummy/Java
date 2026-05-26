package com.example.newjavaproject.assistant;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.newjavaproject.data.SharedPrefsManager;
import com.example.newjavaproject.R;

import java.util.ArrayList;
import java.util.Locale;

public class AssistantFragment extends Fragment implements SensorEventListener {

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

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastMovementTime = 0; 
    private static final int MOVEMENT_CHECK_INTERVAL_MS = 5000;

    private ArrayList<String> dummyHistoryList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timeHandler = new Handler(Looper.getMainLooper());
        prefsManager = new SharedPrefsManager(requireContext());

        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
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
            saveHistoryToPrefs(); // 清空後也要同步寫入儲存空間，否則重開App紀錄又會回來
            updateHistoryUI();
            Toast.makeText(requireContext(), "歷史紀錄已清空", Toast.LENGTH_SHORT).show();
        });

        // 【優化點 1】載入進度與歷史紀錄，並刷新 UI
        loadSavedProgress();
        loadHistoryFromPrefs(); // 先從本地載入歷史紀錄資料
        updateHistoryUI();      // 再繪製到畫面上

        timeRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    currentSessionTime++;
                    currentDistanceInKm = currentSessionTime * 0.0012; 
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
                        return; // 達標停止後直接中斷，不要再往下執行 postDelayed
                    }
                    
                    // 【優化點 2】修正潛在的計時器重複執行的 Bug
                    // 確保只有在「依然處於播放狀態」且「未達標」的情況下才繼續推進下一秒
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

    private void saveHistoryToPrefs() {
        if (getContext() == null) return;
        android.content.SharedPreferences sp = requireContext().getSharedPreferences("RunningPrefs", Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (String s : dummyHistoryList) {
            sb.append(s).append("|||");
        }
        sp.edit().putString("history_records", sb.toString()).apply();
    }

    private void loadHistoryFromPrefs() {
        if (getContext() == null) return;
        android.content.SharedPreferences sp = requireContext().getSharedPreferences("RunningPrefs", Context.MODE_PRIVATE);
        String savedRecords = sp.getString("history_records", "");
        
        dummyHistoryList.clear(); 
        
        if (savedRecords != null && !savedRecords.isEmpty()) {
            String[] parts = savedRecords.split("\\|\\|\\|");
            for (String s : parts) {
                if (!s.isEmpty()) { 
                    dummyHistoryList.add(s);
                }
            }
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

    private void startExercise() {
        isPlaying = true;
        btnStartStop.setText("STOP");
        btnStartStop.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null));
        btnResetTarget.setVisibility(View.GONE);
        tvStatusHint.setText("運動中...請維持步伐與呼吸");
        lastMovementTime = System.currentTimeMillis();
        tvPoseReminderStatus.setText("穩定維持");
        tvPoseReminderStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (currentSessionTime > 5) {
            double calories = currentDistanceInKm * 60.0;
            String newRecord = String.format(Locale.getDefault(), "🔥 時間: %02d:%02d | 里程: %.2f km | 消耗: %.1f kcal",
                    currentSessionTime / 60, currentSessionTime % 60, currentDistanceInKm, calories);
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

    private void loadSavedProgress() {
        if (getContext() == null) return;
        android.content.SharedPreferences sp = requireContext().getSharedPreferences("RunningPrefs", android.content.Context.MODE_PRIVATE);
        targetTimeInSeconds = sp.getLong("target_time", 0);
        targetDistanceInKm = Double.longBitsToDouble(sp.getLong("target_distance", Double.doubleToLongBits(0.0)));
        currentSessionTime = sp.getLong("current_time", 0);
        currentDistanceInKm = currentSessionTime * 0.0012;
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
        if (isPlaying) { stopExerciseAndUpdateUI(); }
    }
}