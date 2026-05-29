package com.example.newjavaproject.map.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class WeatherApiClient {

    // 建立一個 Callback 介面，讓資料抓完後可以傳回給 MapFragment
    public interface WeatherCallback {
        void onSuccess(String wx, String minT, String maxT);
        void onError(String errorMessage);
    }

    public void fetchWeather(String countyName, String apiKey, WeatherCallback callback) {
        // 網路請求必須在背景執行緒 (Background Thread) 執行，不能卡住畫面
        new Thread(() -> {
            try {
                // 1. 處理縣市名稱 (如果是中文，網址必須經過 UTF-8 編碼)
                String encodedCounty = URLEncoder.encode(countyName, "UTF-8");

                // 2. 組合氣象署的 API 網址 (加上 format=JSON 確保回傳是 JSON 格式)
                String urlString = "https://opendata.cwa.gov.tw/api/v1/rest/datastore/F-C0032-001" +
                        "?Authorization=" + apiKey +
                        "&format=JSON" +
                        "&locationName=" + encodedCounty;

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // 設定連線超時為 5 秒
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 3. 讀取回傳的資料
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // 4. 解析 JSON 資料
                    JSONObject root = new JSONObject(response.toString());
                    JSONArray locations = root.getJSONObject("records").getJSONArray("location");

                    if (locations.length() > 0) {
                        JSONArray weatherElements = locations.getJSONObject(0).getJSONArray("weatherElement");
                        
                        String wx = "";
                        String minT = "";
                        String maxT = "";

                        // 尋找我們需要的 Wx, MinT, MaxT
                        for (int i = 0; i < weatherElements.length(); i++) {
                            JSONObject element = weatherElements.getJSONObject(i);
                            String elementName = element.getString("elementName");

                            /* * 💡 關於時間的說明：
                             * 氣象署的 F-C0032-001 (36小時預報) 必定回傳 3 筆時間資料。
                             * 陣列的第一筆 ( index 0 )，永遠都是「當下這個時刻到接下來12小時」的最新預報。
                             * 所以我們直接抓 time.getJSONObject(0) 就是最即時的資料，不需要自己切月份日期來比對！
                             */
                            JSONObject firstTimeBlock = element.getJSONArray("time").getJSONObject(0);
                            String paramName = firstTimeBlock.getJSONObject("parameter").getString("parameterName");

                            if (elementName.equals("Wx")) {
                                wx = paramName;
                            } else if (elementName.equals("MinT")) {
                                minT = paramName;
                            } else if (elementName.equals("MaxT")) {
                                maxT = paramName;
                            }
                        }

                        // 5. 資料準備好了！切換回主執行緒 (UI Thread) 更新畫面
                        final String finalWx = wx;
                        final String finalMinT = minT;
                        final String finalMaxT = maxT;
                        
                        new Handler(Looper.getMainLooper()).post(() -> 
                                callback.onSuccess(finalWx, finalMinT, finalMaxT)
                        );
                    } else {
                        // 找不到該縣市的資料
                        new Handler(Looper.getMainLooper()).post(() -> 
                                callback.onError("找不到氣象資料")
                        );
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> 
                            callback.onError("伺服器回應錯誤: " + responseCode)
                    );
                }
                connection.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("WEATHER_API", "連線或解析失敗: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onError("網路或解析發生錯誤")
                );
            }
        }).start();
    }
}