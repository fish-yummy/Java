package com.example.newjavaproject.assistant;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.example.newjavaproject.data.SharedPrefsManager;
import com.example.newjavaproject.R;

public class AssistantFragment extends Fragment {

  
    private Handler beatHandler;
    private Runnable beatRunnable;
    private SharedPrefsManager prefsManager;

    // 狀態變數
    private boolean isPlaying = false;
    private long currentSessionTime = 0; // 本次運動秒數

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化 Handler 
        beatHandler = new Handler(Looper.getMainLooper());
        // 初始化SharedPrefsManager
        prefsManager = new SharedPrefsManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 綁定 (假設 XML 中有這些元件)
        Button btnStartStop = view.findViewById(R.id.btn_start_stop);
        TextView tvTimeDisplay = view.findViewById(R.id.tv_time_display);

        // [組員任務] 實作節拍器核心邏輯
        beatRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    // TODO: 1. 播放音效
                    
                    // TODO: 2. currentSessionTime 加 1 秒，並更新 tvTimeDisplay 畫面
                    
                    // 3. 呼叫自己，形成迴圈 (假設超慢跑頻率為 180 BPM，約每 333 毫秒響一次)
                    // TODO: 將 1000 替換成使用者設定的頻率變數
                    beatHandler.postDelayed(this, 1000); 
                }
            }
        };

        btnStartStop.setOnClickListener(v -> {
            if (!isPlaying) {
                // 開始運動
                isPlaying = true;
                beatHandler.post(beatRunnable); // 啟動節拍器
                btnStartStop.setText("停止運動");
            } else {
                // 停止運動
                stopMetronomeAndSaveData();
                btnStartStop.setText("開始運動");
            }
        });
    }

    // ==========================================
    // 架構師的防護網：生命週期管理
    // ==========================================
    @Override
    public void onPause() {
        super.onPause();
        // 當使用者切換到「氧森地圖」或退回桌面時，強制停止並存檔
        if (isPlaying) {
            stopMetronomeAndSaveData();
            // TODO: (UI) 若有抓到 Button，記得把按鈕文字改回 "開始運動"
        }
    }

    private void stopMetronomeAndSaveData() {
        isPlaying = false;
        // 清除尚未執行的節拍，防止 Memory Leak
        beatHandler.removeCallbacksAndMessages(null); 
        
        // 呼叫 SharedPrefsManager 儲存 currentSessionTime 與更新天數
        // TODO: prefsManager.addExerciseTime(currentSessionTime);
        // ... (記得儲存完將 currentSessionTime 歸零)
    }
}