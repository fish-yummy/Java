package com.example.newjavaproject;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import com.example.newjavaproject.map.MapFragment;

public class MainActivity extends AppCompatActivity {

    

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

     
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            loadFragment(new MapFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_map) {
                //tvContent.setText("氧森地圖畫面");
                loadFragment(new MapFragment());
                return true;
            } else if (itemId == R.id.nav_assistant) {
                Toast.makeText(MainActivity.this, "慢動助手畫面", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_nutrition) {
                Toast.makeText(MainActivity.this, "食運天平畫面", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }


    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}