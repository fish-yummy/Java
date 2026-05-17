package com.example.newjavaproject.data; 

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {

    // 檔案名稱與欄位 Key
    private static final String PREF_NAME = "EcoStep_SlowMotion_Prefs";
    private static final String KEY_TOTAL_TIME = "TotalExerciseTime";
    private static final String KEY_EXERCISE_DAYS = "ExerciseDays";
    private static final String KEY_LAST_EXERCISE_DATE = "LastExerciseDate";

    private SharedPreferences sharedPreferences;

    // 建構子
    public SharedPrefsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ==========================================
    // 給 UI 層呼叫的方法
    // ==========================================

    /**
     *  儲存單次運動時間，並累加到總時長
     * @param durationInSeconds 這次運動了幾秒
     */
    public void addExerciseTime(long durationInSeconds) {
        // TODO: 1. 取出目前的總時長 (getInt 或 getLong)
        // TODO: 2. 加上 durationInSeconds
        // TODO: 3. 使用 sharedPreferences.edit().putXXX().apply() 存回系統
    }

    /**
     *判斷今天是否已經運動過，若無則增加運動天數
     * @param currentDate 今天的日期字串 (例如 "2026-05-17")
     */
    public void checkAndUpdateExerciseDays(String currentDate) {
        // TODO: 1. 讀取 KEY_LAST_EXERCISE_DATE，比對是否等於 currentDate
        // TODO: 2. 若不等於，代表今天是新的一天，把 KEY_EXERCISE_DAYS 數值 +1
        // TODO: 3. 將 KEY_LAST_EXERCISE_DATE 更新為 currentDate
    }

    // TODO: 新增 get 獲取總時長與天數的方法，供畫面上 TextView 顯示用
}