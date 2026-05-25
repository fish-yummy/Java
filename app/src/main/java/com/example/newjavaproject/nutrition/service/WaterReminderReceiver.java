package com.example.newjavaproject.nutrition.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class WaterReminderReceiver extends BroadcastReceiver {
    
    // 定義通知管道的 ID (就像是收音機的頻道)
    private static final String CHANNEL_ID = "water_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        // 💡 關鍵就在這：Android 8.0 以上必須手動建立通知管道，否則通知絕對發不出來！
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "智慧飲水提醒", // 在手機設定裡看到的通知類別名稱
                    NotificationManager.IMPORTANCE_HIGH // 設定為高重要度，確保會跳出橫幅
            );
            channel.setDescription("定時提醒運動族群補充水分");
            manager.createNotificationChannel(channel);
        }

        // 建立通知內容 (記得 Channel ID 要跟上面一模一樣)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // 使用內建通知圖示
                .setContentTitle("🥤 食運天平：補水時間到囉！")
                .setContentText("根據您今天的運動強度，現在該補充 300ml 的水分了。")
                .setPriority(NotificationCompat.PRIORITY_HIGH) // 確保舊版手機也能跳出橫幅
                .setAutoCancel(true);

        // 發出通知 (ID 設為 1)
        manager.notify(1, builder.build());
    }
}