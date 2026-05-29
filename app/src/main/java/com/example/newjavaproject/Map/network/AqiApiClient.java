package com.example.newjavaproject.map.network;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AqiApiClient {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 更新 Callback：加入 colorHex 和 countyName 以便 UI 變色與顯示
    public interface AqiCallback {
        void onSuccess(String pm25Value, String status, String colorHex, String countyName);
        void onError(String errorMessage);
    }

    // 傳入定位到的「縣市名稱」與你的 API KEY
    public void fetchAveragePm25(String targetCounty, String apiKey, AqiCallback callback) {
        executor.execute(() -> {
            try {
                // 替換為 PM2.5 一般測站的 API
                URL url = new URL("https://data.moenv.gov.tw/api/v2/aqx_p_02?api_key=" + apiKey);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                // 讀取 JSON 回傳字串
                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String jsonString = response.toString().trim();
                JSONArray records;

                // 【修改這裡】加入彈性的解析邏輯：判斷是陣列還是物件
                if (jsonString.startsWith("{")) {
                    // 如果是大括號開頭 (標準物件格式)
                    JSONObject root = new JSONObject(jsonString);
                    records = root.getJSONArray("records");
                } else if (jsonString.startsWith("[")) {
                    // 如果是中括號開頭 (直接是陣列格式，也就是你目前遇到的情況)
                    records = new JSONArray(jsonString);
                } else {
                    throw new Exception("未知的 JSON 格式");
                }

                int totalPm25 = 0;
                int validStationCount = 0;

                // 跑迴圈比對縣市，並過濾例外資料
                for (int i = 0; i < records.length(); i++) {
                    JSONObject obj = records.getJSONObject(i);
                    String county = obj.optString("county", "");

                    if (county.equals(targetCounty)) {
                        String pm25Str = obj.optString("pm25", "").trim();
                        
                        // 【防呆判斷式】避開空值或維修中的 "ND"
                        if (!pm25Str.isEmpty() && !pm25Str.equalsIgnoreCase("ND")) {
                            try {
                                totalPm25 += Integer.parseInt(pm25Str);
                                validStationCount++;
                            } catch (NumberFormatException e) {
                                // 略過無法解析的非數字資料
                            }
                        }
                    }
                }

                if (validStationCount > 0) {
                    // 計算平均值
                    int averagePm25 = totalPm25 / validStationCount;
                    
                    // 根據平均值取得狀態文字與顏色
                    String[] statusAndColor = getStatusAndColor(averagePm25);
                    
                    callback.onSuccess(String.valueOf(averagePm25), statusAndColor[0], statusAndColor[1], targetCounty);
                } else {
                    callback.onError("該地區目前無有效的 PM2.5 測站資料");
                }

            } catch (Exception e) {
                callback.onError("網路或解析發生錯誤：" + e.getMessage());
            }
        });
    }

    // 將數值轉換為對應的等級與顏色 (Hex Code)
    private String[] getStatusAndColor(int pm25) {
        if (pm25 <= 15) return new String[]{"良好", "#4CAF50"};          // 綠
        if (pm25 <= 35) return new String[]{"普通", "#FBC02D"};          // 黃 (稍微加深以利閱讀)
        if (pm25 <= 54) return new String[]{"對敏感族群不健康", "#FF9800"};// 橘
        if (pm25 <= 150) return new String[]{"對所有族群不健康", "#F44336"};// 紅
        if (pm25 <= 250) return new String[]{"非常不健康", "#9C27B0"};    // 紫
        return new String[]{"危害", "#800000"};                          // 褐紅
    }
}