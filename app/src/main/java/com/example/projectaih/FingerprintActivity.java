package com.example.projectaih;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.MqttException;

public class FingerprintActivity extends AppCompatActivity {

    private static final String TAG = "FingerprintActivity";
    private static final String MQTT_CONTROL_TOPIC = "esp32/control"; // 控制命令的主题

    private Button enrollButton;
    private Button deleteButton;
    private EditText deleteIdEditText;  // 用于输入要删除的指纹 ID
    private TextView statusTextView;     // 用于显示状态信息

    // 通过接口与 MainActivity 通信
    //private MqttActionListener mqttActionListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);

        enrollButton = findViewById(R.id.enrollButton);
        deleteButton = findViewById(R.id.deleteButton);
        deleteIdEditText = findViewById(R.id.deleteIdEditText);
        statusTextView = findViewById(R.id.fingerprintStatusTextView);

        enrollButton.setOnClickListener(v -> {
            // 发送 MQTT 消息，开始录入指纹
            sendMqttCommand("enroll", "start"); // 使用 "start" 作为值，表示开始录入
        });

        deleteButton.setOnClickListener(v -> {
            // 获取要删除的指纹 ID
            String idString = deleteIdEditText.getText().toString().trim();
            if (!idString.isEmpty()) {
                try {
                    int id = Integer.parseInt(idString);
                    // 发送 MQTT 消息，删除指纹
                    sendMqttCommand("delete", String.valueOf(id)); // 将 ID 作为值发送
                } catch (NumberFormatException e) {
                    showToast("Invalid ID format"); // ID 格式不正确
                }
            } else {
                showToast("Please enter an ID to delete"); // 未输入 ID
            }
        });
    }

    // 通过广播发送 MQTT 消息 (使用 MainActivity 中的 sendMqttMessage)
    private void sendMqttCommand(String action, String value) {
        //发送广播
        Intent intent = new Intent("com.example.projectaih.ACTION_MQTT_COMMAND");
        intent.putExtra("action", action);
        intent.putExtra("value", value);
        sendBroadcast(intent);

    }

    private void showToast(final String message){
        runOnUiThread(() -> Toast.makeText(FingerprintActivity.this,message,Toast.LENGTH_LONG).show());
    }

    // 你可以添加一个方法来更新状态 TextView (例如，显示指纹录入/删除的结果)
    public void updateStatus(String status) {
        runOnUiThread(() -> statusTextView.setText(status));
    }

    // 你可以在这里添加一个 BroadcastReceiver 来接收来自 ESP32 的 MQTT 消息 (如果需要)
    // 并根据消息内容更新 UI (例如，显示指纹录入的进度、结果等)
}