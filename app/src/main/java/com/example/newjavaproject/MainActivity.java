package com.example.newjavaproject;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.widget.Toast;

import android.content.Intent;
import android.os.Bundle;
import com.example.newjavaproject.map.MapActivity;


public class MainActivity extends AppCompatActivity {

   
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnMap = findViewById(R.id.btn_map);
        Button btnAssistant = findViewById(R.id.btn_assistant);
        Button btnNutrition = findViewById(R.id.btn_nutrition);

        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        });

        btnAssistant.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "慢動助手開發中...", Toast.LENGTH_SHORT).show();
        });

        btnNutrition.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "食運天平開發中...", Toast.LENGTH_SHORT).show();
        });

        // Intent intent = new Intent(this, MapActivity.class);
        // startActivity(intent);
    }
}