package com.example.projectaih;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast; // 确保 Toast 被导入
import androidx.annotation.NonNull; // 确保这个导入存在
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashSet; // 用于清空 Set

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SensorDataPrefs";
    private static final String PREFS_EVENTS_KEY = "historical_events";
    private static final String PREFS_PROCESSED_EVENT_IDS_KEY = "processed_event_ids";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button clearSensorHistoryButton = findViewById(R.id.clearSensorHistoryButton);
        Button clearEventHistoryButton = findViewById(R.id.clearEventHistoryButton);

        clearSensorHistoryButton.setOnClickListener(v -> {
            clearSensorHistoricalData(); // 清空传感器历史极值
        });

        clearEventHistoryButton.setOnClickListener(v -> {
            clearEventNotificationHistory(); // 清空事件通知历史
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.bottom_home) {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.bottom_history) {
                Intent historyIntent = new Intent(SettingsActivity.this, HistoryActivity.class);
                startActivity(historyIntent);
                return true;
            } else if (itemId == R.id.bottom_menu) {
                Intent menuIntent = new Intent(SettingsActivity.this, MenuActivity.class);
                startActivity(menuIntent);
                return true;
            }else if (itemId == R.id.bottom_settings){
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.bottom_settings);
    }

    // 清空传感器历史极值数据
    private void clearSensorHistoricalData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 列出所有传感器历史极值的键名进行移除
        editor.remove("max_temp");
        editor.remove("min_temp");
        editor.remove("max_humid");
        editor.remove("min_humid");
        editor.remove("max_light");
        editor.remove("min_light");
        editor.remove("max_pressure");
        editor.remove("min_pressure");
        editor.remove("max_co");
        editor.remove("min_co");
        editor.remove("max_methane");
        editor.remove("min_methane");
        editor.remove("max_smoke");
        editor.remove("min_smoke");
        editor.remove("max_tvoc");
        editor.remove("min_tvoc");
        editor.remove("max_eco2");
        editor.remove("min_eco2");
        editor.remove("max_rain");
        editor.remove("min_rain");

        editor.remove("mq7_raw");
        editor.remove("mq4_raw");
        editor.remove("mq2_raw");

        editor.apply();

        Toast.makeText(this, getString(R.string.sensor_history_cleared), Toast.LENGTH_SHORT).show();
    }

    //清空历史事件通知
    private void clearEventNotificationHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 清空事件列表
        editor.remove(PREFS_EVENTS_KEY);
        //清空已处理的 eventId 记录
        editor.remove(PREFS_PROCESSED_EVENT_IDS_KEY);
        editor.apply();

        Toast.makeText(this, getString(R.string.event_history_cleared), Toast.LENGTH_SHORT).show();
    }
}
