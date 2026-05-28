package com.example.newjavaproject.map.network;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OverpassApiClient {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 建立一個簡單的資料結構，用來存放解析後的「點位名稱」與「經緯度」
    public static class ParkLocation {
        public String name;
        public double lat;
        public double lon;

        public ParkLocation(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    // 用來把抓到的資料回傳給畫面的 Callback 介面
    public interface OverpassCallback {
        void onSuccess(List<ParkLocation> locations);
        void onError(String errorMessage);
    }

    // 開放給 UI 呼叫的方法，傳入中心點(長輩)的經緯度
    public void fetchNearbyParks(double lat, double lon, OverpassCallback callback) {
        executor.execute(() -> {
            try {
                // 1. 準備 Overpass QL 語法 (尋找 1000 公尺內的公園與學校)
                // [out:json]：告訴伺服器回傳 JSON 格式
                // out center：如果是多邊形(例如一大片操場)，直接幫我們算出中心點座標
                String query = "[out:json][timeout:15];\n" +
                        "(\n" +
                        "  node[\"leisure\"=\"park\"](around:1000," + lat + "," + lon + ");\n" +
                        "  way[\"leisure\"=\"park\"](around:1000," + lat + "," + lon + ");\n" +
                        "  node[\"amenity\"=\"school\"](around:1000," + lat + "," + lon + ");\n" +
                        "  way[\"amenity\"=\"school\"](around:1000," + lat + "," + lon + ");\n" +
                        ");\n" +
                        "out center;";

                // 2. 設定 URL 並開啟 HttpURLConnection (Overpass 官方伺服器)
                URL url = new URL("https://overpass-api.de/api/interpreter");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); // 查詢字串可能很長，用 POST 比較安全
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // 3. 將查詢語法寫入並發送給伺服器
                try (OutputStream os = conn.getOutputStream()) {
                    // 必須經過 URLEncoder 編碼，伺服器才看得懂
                    byte[] input = ("data=" + URLEncoder.encode(query, "UTF-8")).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // 4. 讀取伺服器回傳的 JSON 結果
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                // 5. 解析 JSON 資料
                List<ParkLocation> resultList = new ArrayList<>();
                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray elements = jsonObject.getJSONArray("elements");

                for (int i = 0; i < elements.length(); i++) {
                    JSONObject element = elements.getJSONObject(i);
                    double elementLat = 0.0;
                    double elementLon = 0.0;

                    // 判斷是單一點(node)還是多邊形(way)，取得對應的經緯度
                    if (element.getString("type").equals("node")) {
                        elementLat = element.getDouble("lat");
                        elementLon = element.getDouble("lon");
                    } else if (element.has("center")) {
                        JSONObject center = element.getJSONObject("center");
                        elementLat = center.getDouble("lat");
                        elementLon = center.getDouble("lon");
                    } else {
                        continue; // 如果沒有座標就跳過這個點
                    }

                    // 取得地點名稱 (有些地方在地圖上沒有標名字，我們給個預設值)
                    String name = "綠色健走點";
                    if (element.has("tags")) {
                        JSONObject tags = element.getJSONObject("tags");
                        if (tags.has("name")) {
                            name = tags.getString("name");
                        }
                    }

                    // 將解析好的點位加入清單
                    resultList.add(new ParkLocation(name, elementLat, elementLon));
                }

                // 6. 成功！將整理好的清單傳回給 UI
                callback.onSuccess(resultList);

            } catch (Exception e) {
                // 發生網路斷線或解析錯誤時
                callback.onError(e.getMessage());
            }
        });
    }
}