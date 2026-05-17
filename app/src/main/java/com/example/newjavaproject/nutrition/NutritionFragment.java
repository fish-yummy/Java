package com.example.newjavaproject.nutrition;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newjavaproject.R;
import com.example.newjavaproject.nutrition.adapter.NutritionAdapter;
import com.example.newjavaproject.nutrition.service.WaterReminderReceiver;

public class NutritionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        
        return inflater.inflate(R.layout.fragment_nutrition, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

   
      
     
        RecyclerView recyclerView = view.findViewById(R.id.recycler_nutrition);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // TODO: 建立假資料清單 (例如：高蛋白餐、香蕉補給等)
        //TODO: 初始化 NutritionAdapter 並綁定到 recyclerView 上
        // NutritionAdapter adapter = new NutritionAdapter(假資料清單);
        // recyclerView.setAdapter(adapter);


        
        // 飲水提醒 (AlarmManager)
        Button btnSetWaterReminder = view.findViewById(R.id.btn_set_water_reminder);
        btnSetWaterReminder.setOnClickListener(v -> {
            // 設定 30 分鐘後推播提醒
            scheduleWaterReminder(30); 
        });
    }

    /**
     *  提醒 呼叫，供 UI 層使用
     */
    private void scheduleWaterReminder(int minutes) {
        Context context = requireContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
       
        Intent intent = new Intent(context, WaterReminderReceiver.class);
        // 使用 FLAG_IMMUTABLE 
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

      
        long triggerAtMillis = System.currentTimeMillis() + ((long) minutes * 60 * 1000);

        // 設定鬧鐘
        if (alarmManager != null) {
           
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Toast.makeText(context, "已設定 " + minutes + " 分鐘後提醒飲水", Toast.LENGTH_SHORT).show();
        }
    }
}