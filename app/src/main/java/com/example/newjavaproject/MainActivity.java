package com.example.newjavaproject;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;

import com.example.newjavaproject.map.MapFragment;
import com.example.newjavaproject.assistant.AssistantFragment;
import com.example.newjavaproject.nutrition.NutritionFragment;

public class MainActivity extends AppCompatActivity {

    // 1. 將 3 個 Fragment 宣告為全域變數，確保它們只會被建立一次
    private final Fragment mapFragment = new MapFragment();
    private final Fragment assistantFragment = new AssistantFragment();
    private final Fragment nutritionFragment = new NutritionFragment();
    
    // 2. 宣告一個變數來記錄目前正在顯示的 Fragment
    private Fragment activeFragment = mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 3. 初始設定：把 3 個 Fragment 都加進 container，但先隱藏另外兩個
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, nutritionFragment, "nutrition").hide(nutritionFragment)
                    .add(R.id.fragment_container, assistantFragment, "assistant").hide(assistantFragment)
                    .add(R.id.fragment_container, mapFragment, "map")
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // 4. 點擊時，呼叫 switchFragment 方法來切換顯示狀態
            if (itemId == R.id.nav_map) {
                switchFragment(mapFragment);
                return true;
            } else if (itemId == R.id.nav_assistant) {
                switchFragment(assistantFragment);
                return true;
            } else if (itemId == R.id.nav_nutrition) {
                switchFragment(nutritionFragment);
                return true;
            }
            return false;
        });
    }

    // 5. 統一處理 Fragment 切換的邏輯
    private void switchFragment(Fragment targetFragment) {
        // 如果點擊的不是當前正在顯示的 Fragment，才進行切換
        if (activeFragment != targetFragment) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .hide(activeFragment)  // 隱藏舊的
                    .show(targetFragment)  // 顯示新的
                    .commit();
            
            // 更新 activeFragment 的紀錄
            activeFragment = targetFragment;
        }
    }
}