package com.example.newjavaproject.map.network;

import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//
public class AqiApiClient {

    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 用來把抓到的資料回傳給畫面
    public interface AqiCallback {
        void onSuccess(String aqiValue, String status); // 成功時回傳 AQI 和狀態(良好/普通)
        void onError(String errorMessage);              // 失敗時回傳錯誤訊息
    }


    // 開放給 UI 呼叫的方法：開始抓取 AQI
    public void fetchCurrentAqi(AqiCallback callback) {
        
        executor.execute(() -> {
            try {
                // 設定 URL 並開啟 HttpURLConnection
                URL url = new URL("這裡填入政府_AQI_API_的網址");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // 讀取 InputStream，拿到 JSON 字串
                String jsonResponse = "{ ... 假裝這是抓下來的 JSON ... }"; 

                // 使用 JSONObject 解析需要的數字 
                String parsedAqi = "42";      // 這是解析後的假資料
                String parsedStatus = "良好"; // 這是解析後的假資料

          
                callback.onSuccess(parsedAqi, parsedStatus);

            } catch (Exception e) {
                // 發生網路斷線或解析錯誤時
                callback.onError(e.getMessage());
            }
        });
    }
}