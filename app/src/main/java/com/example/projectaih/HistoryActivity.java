package com.example.projectaih;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // 添加日志导入
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull; // 确保这个导入存在
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONException; // 添加 JSONException 导入
import org.json.JSONObject;  // 添加 JSONObject 导入
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HistoryActivity extends AppCompatActivity {

    // 声明 TextView 变量 (用于历史极值)
    private TextView maxTempTextView, minTempTextView;
    private TextView maxHumidTextView, minHumidTextView;
    private TextView maxLightTextView, minLightTextView;
    private TextView maxPressureTextView, minPressureTextView;
    private TextView maxCoPpmTextView, minCoPpmTextView;
    private TextView maxMethaneTextView, minMethaneTextView; // 新增
    private TextView maxSmokeTextView, minSmokeTextView;     // 新增
    private TextView maxRainTextView, minRainTextView;       // 新增
    private TextView maxTvocTextView, minTvocTextView;       // 新增
    private TextView maxEco2TextView, minEco2TextView;       // 新增

    // 事件历史相关的 UI 和数据
    private ListView eventHistoryListView;
    private ArrayAdapter<String> eventAdapter;
    private List<String> eventDisplayList;

    // SharedPreferences 文件名和键名
    private static final String PREFS_NAME = "SensorDataPrefs";
    private static final String PREFS_EVENTS_KEY = "historical_events";
    private static final String TAG = "HistoryActivity"; // 用于日志

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 初始化历史极值的 TextView
        maxTempTextView = findViewById(R.id.maxTempTextView);
        minTempTextView = findViewById(R.id.minTempTextView);
        maxHumidTextView = findViewById(R.id.maxHumidTextView);
        minHumidTextView = findViewById(R.id.minHumidTextView);
        maxLightTextView = findViewById(R.id.maxLightTextView);
        minLightTextView = findViewById(R.id.minLightTextView);
        maxPressureTextView = findViewById(R.id.maxPressureTextView);
        minPressureTextView = findViewById(R.id.minPressureTextView);
        maxCoPpmTextView = findViewById(R.id.maxCoPpmTextView);
        minCoPpmTextView = findViewById(R.id.minCoPpmTextView);
        maxMethaneTextView = findViewById(R.id.maxMethaneTextView);
        minMethaneTextView = findViewById(R.id.minMethaneTextView);
        maxSmokeTextView = findViewById(R.id.maxSmokeTextView);
        minSmokeTextView = findViewById(R.id.minSmokeTextView);
        maxRainTextView = findViewById(R.id.maxRainTextView);
        minRainTextView = findViewById(R.id.minRainTextView);
        maxTvocTextView = findViewById(R.id.maxTvocTextView);
        minTvocTextView = findViewById(R.id.minTvocTextView);
        maxEco2TextView = findViewById(R.id.maxEco2TextView);
        minEco2TextView = findViewById(R.id.minEco2TextView);

        // 初始化事件历史的 ListView
        eventHistoryListView = findViewById(R.id.eventHistoryListView);
        eventDisplayList = new ArrayList<>();
        eventAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, eventDisplayList);
        eventHistoryListView.setAdapter(eventAdapter);

        // 加载并显示数据
        loadHistoricalData();

        // 初始化 BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.bottom_history); // 设置当前页面为选中状态


// 设置 BottomNavigationView 的选中监听器
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.bottom_home) {
                Intent homeIntent = new Intent(HistoryActivity.this, MainActivity.class);
                startActivity(homeIntent);
                return true;
            } else if (itemId == R.id.bottom_history) {
                return true;
            } else if (itemId == R.id.bottom_menu) {
                Intent menuIntent = new Intent(HistoryActivity.this, MenuActivity.class);
                startActivity(menuIntent);
                return true;
            } else if (itemId == R.id.bottom_settings) {
                Intent musicIntent = new Intent(HistoryActivity.this, SettingsActivity.class);
                startActivity(musicIntent);
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.bottom_history);

    }

    private void loadHistoricalData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String maxPrefix = getString(R.string.max_prefix); // "最高: "
        String minPrefix = getString(R.string.min_prefix); // "最低: "

        // 加载并显示最大/最小值
        maxTempTextView.setText(String.format(Locale.getDefault(), "%s%.1f °C", maxPrefix, prefs.getFloat("max_temp", 0)));
        minTempTextView.setText(String.format(Locale.getDefault(), "%s%.1f °C", minPrefix, prefs.getFloat("min_temp", 0)));

        maxHumidTextView.setText(String.format(Locale.getDefault(), "%s%.1f %%", maxPrefix, prefs.getFloat("max_humid", 0)));
        minHumidTextView.setText(String.format(Locale.getDefault(), "%s%.1f %%", minPrefix, prefs.getFloat("min_humid", 0)));

        maxLightTextView.setText(String.format(Locale.getDefault(), "%s%.1f lx", maxPrefix, prefs.getFloat("max_light", 0)));
        minLightTextView.setText(String.format(Locale.getDefault(), "%s%.1f lx", minPrefix, prefs.getFloat("min_light", 0)));

        maxPressureTextView.setText(String.format(Locale.getDefault(), "%s%.1f hPa", maxPrefix, prefs.getFloat("max_pressure", 0)));
        minPressureTextView.setText(String.format(Locale.getDefault(), "%s%.1f hPa", minPrefix, prefs.getFloat("min_pressure", 0)));

        maxCoPpmTextView.setText(String.format(Locale.getDefault(), "%s%.1f ppm", maxPrefix, prefs.getFloat("max_co", 0))); // 假设 co 是 max_co
        minCoPpmTextView.setText(String.format(Locale.getDefault(), "%s%.1f ppm", minPrefix, prefs.getFloat("min_co", 0))); // 假设 co 是 min_co

        maxMethaneTextView.setText(String.format(Locale.getDefault(), "%s%.1f ppm", maxPrefix, prefs.getFloat("max_methane", 0)));
        minMethaneTextView.setText(String.format(Locale.getDefault(), "%s%.1f ppm", minPrefix, prefs.getFloat("min_methane", 0)));

        maxSmokeTextView.setText(String.format(Locale.getDefault(), "%s%.1f ppm", maxPrefix, prefs.getFloat("max_smoke", 0)));
        minSmokeTextView.setText(String.format(Locale.getDefault(), "%s%.1f ppm", minPrefix, prefs.getFloat("min_smoke", 0)));

        maxRainTextView.setText(String.format(Locale.getDefault(), "%s%.1f %%", maxPrefix, prefs.getFloat("max_rain", 0)));
        minRainTextView.setText(String.format(Locale.getDefault(), "%s%.1f %%", minPrefix, prefs.getFloat("min_rain", 0)));

        maxTvocTextView.setText(String.format(Locale.getDefault(), "%s%d ppb", maxPrefix, prefs.getInt("max_tvoc", 0))); // TVOC 是 Integer
        minTvocTextView.setText(String.format(Locale.getDefault(), "%s%d ppb", minPrefix, prefs.getInt("min_tvoc", 0)));

        maxEco2TextView.setText(String.format(Locale.getDefault(), "%s%d ppm", maxPrefix, prefs.getInt("max_eco2", 0))); // eCO2 是 Integer
        minEco2TextView.setText(String.format(Locale.getDefault(), "%s%d ppm", minPrefix, prefs.getInt("min_eco2", 0)));


        // 加载并显示历史事件通知
        Set<String> eventsSet = prefs.getStringSet(PREFS_EVENTS_KEY, new HashSet<>());
        List<String> rawEventsList = new ArrayList<>(eventsSet);
        List<EventItem> parsedEvents = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());

        for (String eventJsonString : rawEventsList) {
            try {
                JSONObject eventJson = new JSONObject(eventJsonString);
                long timestamp = eventJson.optLong("timestamp", System.currentTimeMillis()); // 提供默认值
                String description = eventJson.optString("description", "未知事件"); // 提供默认值
                parsedEvents.add(new EventItem(timestamp, description));
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing event JSON from prefs: " + eventJsonString, e);
            }
        }

        // 按时间戳降序排序 (最新的在最前面)
        Collections.sort(parsedEvents, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

        eventDisplayList.clear();
        for (EventItem item : parsedEvents) {
            String formattedTime = sdf.format(new Date(item.getTimestamp()));
            eventDisplayList.add(formattedTime + " - " + item.getDescription());
        }
        eventAdapter.notifyDataSetChanged();
    }

    // 简单的内部类来帮助排序事件
    private static class EventItem {
        private long timestamp;
        private String description;

        public EventItem(long timestamp, String description) {
            this.timestamp = timestamp;
            this.description = description;
        }
        public long getTimestamp() { return timestamp; }
        public String getDescription() { return description; }
    }
}
