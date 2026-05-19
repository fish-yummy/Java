package com.example.newjavaproject.nutrition;

// 這是一個資料模型，專門用來打包每一筆補給資料
public class NutritionItem {
    private String title;       // 儲存標題 (例如："🏃‍♂️ 運動前補給")
    private String description; // 儲存詳細說明
    private int imageResId;     // 儲存圖片的代號

    // 這是建構子，讓我們以後可以方便地建立一組資料
    public NutritionItem(String title, String description, int imageResId) {
        this.title = title;
        this.description = description;
        this.imageResId = imageResId;
    }

    // 底下這些是讓加工廠（Adapter）拿資料用的方法
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getImageResId() { return imageResId; }
}