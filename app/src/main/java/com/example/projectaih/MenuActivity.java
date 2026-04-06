package com.example.projectaih;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        MaterialCardView deepseekCard = findViewById(R.id.deepseekCardView);
        MaterialCardView jamendoCard = findViewById(R.id.jamendoCardView);
        // 获取 BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        deepseekCard.setOnClickListener(v -> {
            // 跳转到 ChatActivity
            Intent intent = new Intent(MenuActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        jamendoCard.setOnClickListener(v -> {
            // 跳转到 MusicSearchActivity
            Intent intent = new Intent(MenuActivity.this, MusicSearchActivity.class);
            startActivity(intent);
        });
        // 设置 BottomNavigationView 的选中监听器
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.bottom_home) {
                // 点击 Home 图标，返回 MainActivity, 不需要finish，避免重复创建
                Intent homeIntent = new Intent(MenuActivity.this, MainActivity.class);
                startActivity(homeIntent);
                return true;
            } else if (itemId == R.id.bottom_history) {
                // 启动 HistoryActivity
                Intent historyIntent = new Intent(MenuActivity.this, HistoryActivity.class);
                startActivity(historyIntent);
                return true;
            }  else if (itemId == R.id.bottom_menu) {
                return true;

            } else if (itemId == R.id.bottom_settings) {
                Intent settingsIntent = new Intent(MenuActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            }
            return false;
        });
        // 设置默认选中项为 "Menu"
        bottomNavigationView.setSelectedItemId(R.id.bottom_menu);
    }
}