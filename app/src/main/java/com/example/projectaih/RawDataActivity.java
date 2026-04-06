package com.example.projectaih;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class RawDataActivity extends AppCompatActivity {

    private TextView mq7RawTextView;
    private TextView mq135RawTextView;
    private TextView mq2RawTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_data);

        mq7RawTextView = findViewById(R.id.mq7RawTextView);
        mq135RawTextView = findViewById(R.id.mq135RawTextView);
        mq2RawTextView = findViewById(R.id.mq2RawTextView);

        // 获取从 MainActivity 传递过来的 RAW 数据
        Intent intent = getIntent();
        int mq7Raw = intent.getIntExtra("mq7_raw", -1);
        int mq135Raw = intent.getIntExtra("mq135_raw", -1);
        int mq2Raw = intent.getIntExtra("mq2_raw", -1);

        // 更新 UI
        if (mq7Raw != -1) {
            mq7RawTextView.setText(String.format(Locale.getDefault(), "MQ7 Raw: %d", mq7Raw));
        }
        if (mq135Raw != -1) {
            mq135RawTextView.setText(String.format(Locale.getDefault(), "MQ135 Raw: %d", mq135Raw));
        }
        if (mq2Raw != -1) {
            mq2RawTextView.setText(String.format(Locale.getDefault(), "MQ2 Raw: %d", mq2Raw));
        }
    }
}