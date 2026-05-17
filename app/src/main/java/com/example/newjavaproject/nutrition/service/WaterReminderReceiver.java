package com.example.newjavaproject.nutrition.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.util.Log;

public class WaterReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("WaterReminder", "時間到！準備發送飲水推播。");

        // 實作 Notification 推播邏輯
        // TODO: 1. Android 8.0 以上需要建立 NotificationChannel
        // TODO: 2. 使用 NotificationCompat.Builder 建立通知內容 (設定小圖示、標題、內文)
        // TODO: 3. 呼叫 NotificationManager.notify() 發送通知
        
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // ... 完成推播細節 ...
    }
}