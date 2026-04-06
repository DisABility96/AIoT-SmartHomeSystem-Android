package com.example.projectaih;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.facebook.stetho.BuildConfig;
import com.facebook.stetho.Stetho;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

//已弃用
import android.widget.ToggleButton;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.core.content.ContextCompat;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String MQTT_SERVER = "tcp://broker.emqx.io:1883";
    private static final String EMQX_BROKER_ADDRESS_FOR_DISPLAY = "broker.emqx.io"; // 用于显示的 EMQX 地址
    private static final String MQTT_TOPIC = "esp32/sensorData";
    private static final String MQTT_CONTROL_TOPIC = "esp32/control";  // 控制命令的主题
    private static final String CLIENT_ID = "AndroidClient_" + System.currentTimeMillis();
    private static final int RECONNECT_INTERVAL = 5000; // 5 秒
    private static final int WIFI_UPDATE_INTERVAL = 10000; // 10 秒
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_WIFI_STATE = 123; // 请求码
    private static final String MQTT_EVENT_TOPIC = "esp32/events"; //事件主题
    private static final String NOTIFICATION_CHANNEL_ID = "smart_home_alerts"; // 通知渠道 ID
    private static final String PREFS_PROCESSED_EVENT_IDS_KEY = "processed_event_ids";
    private static final int MAX_PROCESSED_IDS_HISTORY = 50; // 最多记录最近 50 个已处理事件ID的滚动窗口
    private static final int NOTIFICATION_ID_VISITOR = 1; // 访客通知 ID
    private static final int NOTIFICATION_ID_ALARM_CO = 2;
    private static final int NOTIFICATION_ID_ALARM_SMOKE = 3;
    private static final int NOTIFICATION_ID_ALARM_METHANE = 4;
    private static final int NOTIFICATION_ID_ALARM_GENERIC = 5; // 用于其他类型的报警
    private static final int MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 101; // 通知权限请求码
    private static final String PREFS_NAME = "SensorDataPrefs";
    private static final String PREFS_EVENTS_KEY = "historical_events";
    private static final int MAX_EVENT_HISTORY = 20; // 最多存储 20 条事件
    private MqttClient mqttClient;
    private TextView temperatureTextView;
    private TextView humidityTextView;
    private TextView lightTextView;
    private TextView coPpmTextView;
    private TextView pressureTextView;
    private TextView methanePpmTextView;
    private TextView smokePpmTextView;
    private TextView rainLevelTextView;
    private TextView tvocTextView;
    private TextView eco2TextView;
    private TextView pirStatusTextView;
    private TextView wifiIpTextView;
    private TextView emqxStatusTextView;
    private Handler wifiUpdateHandler = new Handler(); // 定时更新 Wi-Fi 信息
    private boolean isConnecting = false;
    private int mq7RawValue = -1;
    private int mq4RawValue = -1;
    private int mq2RawValue = -1;
    private String currentClientId;
    SwitchCompat servoSwitch;
    SwitchCompat stepperSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 动态申请权限，先检查有没有这个权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_WIFI_STATE},
                    MY_PERMISSIONS_REQUEST_ACCESS_WIFI_STATE);
        } else {
            // 权限已授予, 但还是放在授权函数中进行
        }

        // 初始化 UI 元素
        temperatureTextView = findViewById(R.id.temperatureTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        lightTextView = findViewById(R.id.lightTextView);
        coPpmTextView = findViewById(R.id.coPpmTextView);
        pressureTextView = findViewById(R.id.pressureTextView);
        methanePpmTextView = findViewById(R.id.methanePpmTextView);
        wifiIpTextView  = findViewById(R.id.wifiIpTextView);
        emqxStatusTextView  = findViewById(R.id.emqxStatusTextView);
        servoSwitch = findViewById(R.id.servoSwitch);
        stepperSwitch = findViewById(R.id.stepperSwitch);
        methanePpmTextView = findViewById(R.id.methanePpmTextView);
        smokePpmTextView = findViewById(R.id.smokePpmTextView);
        rainLevelTextView = findViewById(R.id.rainLevelTextView);
        tvocTextView = findViewById(R.id.tvocTextView);
        eco2TextView = findViewById(R.id.eco2TextView);
        pirStatusTextView = findViewById(R.id.pirStatusTextView);
        servoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sendMqttMessage("servo", isChecked ? "1" : "0");
        });

        stepperSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sendMqttMessage("stepper", isChecked ? "1" : "0");
        });

        //创建通知渠道 (Android 8.0+)
        createNotificationChannel();
        //检查并请求通知权限 (Android 13+)
        requestNotificationPermission();

        if (savedInstanceState == null) { // 客户端ID：每次都生成新的
            currentClientId = "AndroidClient_" + System.currentTimeMillis();
        } else {
            // 虽然可以尝试恢复 ClientID，但更简单的是每次都用新的
            currentClientId = "AndroidClient_" + System.currentTimeMillis();
        }

        // 连接到 MQTT Broker
        connectToMqttBroker();

        // 初始化 BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.bottom_home) {
                return true;
            } else if (itemId == R.id.bottom_history) {
                Intent historyIntent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(historyIntent);
                return true;
            }else if (itemId == R.id.bottom_menu) {
                Intent menuIntent = new Intent(MainActivity.this, MenuActivity.class);
                startActivity(menuIntent);
                return true;
            } else if (itemId == R.id.bottom_settings) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            }
            return false;
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - MQTT Connected: " + (mqttClient != null && mqttClient.isConnected()));
        // 当Activity返回前台时，检查连接状态并尝试连接
        if (mqttClient != null && !mqttClient.isConnected() && !isConnecting) {
            Log.i(TAG, "onStart: MQTT not connected, attempting to connect via connectToMqttBroker.");
            connectToMqttBroker();// 再次尝试连接
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_WIFI_STATE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，更新Wi-Fi 信息
                updateNetworkStatus();
                startNetworkUpdateTimer();
            } else {
                // 权限被拒绝
                showToast(getString(R.string.permission_wifi_state_denied));
                updateNetworkStatus();
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 通知权限被授予
                showToast(getString(R.string.permission_notification_granted));
            } else {
                // 通知权限被拒绝
                showToast(getString(R.string.permission_notification_denied));
            }
        }
    }
    //连接到EMQX
    private synchronized void connectToMqttBroker() {
        if (isConnecting && mqttClient != null && mqttClient.isConnected()) { // 如果正在连接或者已经连接，则返回
            Log.d(TAG, "Already connecting or connected.");
            return;
        }
        isConnecting = true;
        Log.d(TAG, "Attempting to connect to MQTT Broker with Client ID: " + currentClientId);
        try{
            // 每次都创建新的MqttConnectOptions
            if(mqttClient == null){
                Log.d(TAG, "MqttClient is null, creating new instance.");
                mqttClient = new MqttClient(MQTT_SERVER, currentClientId, new MemoryPersistence());
            } else if (!mqttClient.getClientId().equals(currentClientId) || !mqttClient.getServerURI().equals(MQTT_SERVER)) {
                // 如果 Client ID 或 Server URI 变了，则需要重新创建 MqttClient
                Log.d(TAG, "MqttClient configuration changed, recreating instance.");
                if (mqttClient.isConnected()) {
                    mqttClient.disconnectForcibly();
                }
                mqttClient.close();
                mqttClient = new MqttClient(MQTT_SERVER, currentClientId, new MemoryPersistence());
            }

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true); // 依赖 Paho 的自动重连
            options.setConnectionTimeout(10);    // 连接超时
            options.setKeepAliveInterval(60);  // 心跳间隔

            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    isConnecting = false;
                    if (mqttClient.isConnected()) {
                        Log.d(TAG,"MQTT Connected" + (reconnect? " (Reconnected by Paho)" : ""));
                        subscribeToTopic();
                        updateNetworkStatus();
                    } else {
                        Log.w(TAG, "connectComplete called, but client is not connected. Reconnect: " + reconnect);
                    }
                }

                @Override
                public void connectionLost(Throwable throwable) {
                    isConnecting = false; // 标记连接操作已结束
                    Log.w(TAG,"MQTT Connection lost by Paho", throwable);
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                    handleMqttMessage(topic, mqttMessage); // 调用统一处理
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });

            if(!mqttClient.isConnected()){
                Log.d(TAG, "MqttClient not connected, calling connect().");
                mqttClient.connect(options);
            } else {
                Log.d(TAG, "MqttClient already connected, skipping connect() call.");
                isConnecting = false; // 如果已经连接，重置 isConnecting 标志
            }

        } catch (MqttException e){
            isConnecting = false;
            Log.e(TAG,"Error connecting to MQTT broker",e);
        } catch (Exception e) { // 捕获其他可能的异常
            isConnecting = false;
            Log.e(TAG, "Unexpected error in connectToMqttBroker", e);
        }
    }

    // 手动重连：暂时弃用
    private final Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed()) {
                connectToMqttBroker();
            }
        }
    };
    // 创建通知渠道
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH; // 设置重要性级别
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // 注册渠道到系统
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    //请求通知权限 (Android 13+)
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU 是 Android 13
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                // 直接请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS);
            }
        }
    }
    //订阅主题
    private void subscribeToTopic() {
        try {
            mqttClient.subscribe(MQTT_TOPIC, 0);
            Log.d(TAG, getString(R.string.subscribed_to_topic, MQTT_TOPIC));
            mqttClient.subscribe(MQTT_EVENT_TOPIC, 0); // QoS 0
            Log.d(TAG, "Subscribed to topic: " + MQTT_EVENT_TOPIC);
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, "Error subscribing to topic", e);
            showToast(getString(R.string.error_failed_to_subscribe, e.getMessage()));
        }
    }

    // 发送控制消息
    private void sendMqttMessage(String control, String value) {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                String message = "{\"" + control + "\": " + value + "}";
                mqttClient.publish(MQTT_CONTROL_TOPIC, message.getBytes(), 0, false);
                Log.d(TAG, getString(R.string.mqtt_message_sent, message));
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(TAG, "Error sending MQTT message", e);
                showToast(getString(R.string.error_failed_to_send_mqtt, e.getMessage()));
            }
        } else {
            // 使用 getString(R.string.key)
            showToast(getString(R.string.error_client_not_connected));
        }
    }
    //更新传感器数据
    private void updateSensorData(String payload) {
        try {
            JSONObject jsonObject = new JSONObject(payload);
            // 解析数据
            double temperature = jsonObject.optDouble("temperature", -999.0);
            double humidity = jsonObject.optDouble("humidity", -999.0);
            double light = jsonObject.optDouble("light", -999.0);
            double pressure = jsonObject.optDouble("pressure", -999.0);
            int mq7Raw = jsonObject.optInt("mq7_raw", -1);
            double coPPM = jsonObject.optDouble("co_ppm", -999.0);
            int mq4Raw = jsonObject.optInt("mq4_raw", -1);
            double methanePPM = jsonObject.optDouble("methane_ppm", -999.0);
            int mq2Raw = jsonObject.optInt("mq2_raw", -1);
            double smokePPM = jsonObject.optDouble("smoke_ppm", -999.0);
            int pirStatus = jsonObject.optInt("pir_status", -1);
            int tvoc = jsonObject.optInt("tvoc", -1);
            int eco2 = jsonObject.optInt("eco2", -1);
            int rainRaw = jsonObject.optInt("rain_raw", -1);
            double rainLevel = jsonObject.optDouble("rain_level", -999.0);
            // 保存 RAW 数据
            mq7RawValue = mq7Raw;
            mq4RawValue = mq4Raw;
            mq2RawValue = mq2Raw;
            // 保存历史数据
            saveHistoricalData(temperature, humidity, light, pressure, coPPM, methanePPM, smokePPM, tvoc, eco2, rainLevel);
            runOnUiThread(() -> {
                // 更新 UI
                if (temperature != -999.0) {
                    temperatureTextView.setText(String.format(Locale.getDefault(), getString(R.string.format_temperature), temperature));
                } else {
                    temperatureTextView.setText(R.string.temperature_placeholder);
                }
                if (humidity != -999.0) {
                    humidityTextView.setText(String.format(Locale.getDefault(), getString(R.string.format_humidity), humidity));
                } else {
                    humidityTextView.setText(R.string.humidity_placeholder);
                }
                if (light != -999.0) {
                    lightTextView.setText(String.format(Locale.getDefault(), getString(R.string.format_light), light));
                } else {
                    lightTextView.setText(R.string.light_placeholder);
                }
                if (pressure != -999.0) {
                    pressureTextView.setText(String.format(Locale.getDefault(), getString(R.string.format_pressure), pressure));
                } else {
                    pressureTextView.setText(R.string.pressure_placeholder);
                }
                if (coPPM != -999.0) {
                    coPpmTextView.setText(String.format(Locale.getDefault(), getString(R.string.format_co_ppm), coPPM));
                } else {
                    coPpmTextView.setText(R.string.co_ppm_placeholder);
                }
                if (methanePPM != -999.0) {
                    methanePpmTextView.setText(String.format(Locale.getDefault(), getString(R.string.format_methane_ppm), methanePPM));
                } else {
                    methanePpmTextView.setText(R.string.placeholder_methane_ppm);
                }
                if (smokePPM != -999.0) {
                    smokePpmTextView.setText(String.format(Locale.getDefault(), getString(R.string.format_smoke_ppm), smokePPM));
                } else {
                    smokePpmTextView.setText(R.string.placeholder_smoke_ppm);
                }
                if (rainLevel != -999.0) {
                    rainLevelTextView.setText(String.format(Locale.getDefault(), getString(R.string.format_rain_level), rainLevel));
                } else {
                    rainLevelTextView.setText(R.string.placeholder_rain_level);
                }
                if (tvoc != -1) {
                    tvocTextView.setText(String.format(Locale.getDefault(), getString(R.string.format_tvoc), tvoc));
                } else {
                    tvocTextView.setText(R.string.placeholder_tvoc);
                }
                if (eco2 != -1) {
                    eco2TextView.setText(String.format(Locale.getDefault(), getString(R.string.format_eco2), eco2));
                } else {
                    eco2TextView.setText(R.string.placeholder_eco2);
                }
                if (pirStatus != -1) {
                    String statusText = (pirStatus == 1) ? getString(R.string.pir_detected) : getString(R.string.pir_not_detected);
                    pirStatusTextView.setText(String.format(getString(R.string.format_pir_status), statusText));
                } else {
                    pirStatusTextView.setText(R.string.placeholder_pir_status);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Error parsing JSON", e);
            showToast(getString(R.string.error_parsing_sensor_data_toast));
        }
    }

    //保存历史数据到 SharedPreferences
    private void saveHistoricalData(double temperature, double humidity, double light,
                                    double pressure, double coPPM, double methanePPM,
                                    double smokePPM, int tvoc, int eco2, double rainLevel) {

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        //更新Max/Min
        if (temperature != -999.0) {
            float maxTemp = prefs.getFloat("max_temp", Float.MIN_VALUE);
            float minTemp = prefs.getFloat("min_temp", Float.MAX_VALUE);
            editor.putFloat("max_temp", Math.max(maxTemp, (float) temperature));
            editor.putFloat("min_temp", Math.min(minTemp, (float) temperature));
        }

        if (humidity != -999.0) {
            float maxHumid = prefs.getFloat("max_humid", Float.MIN_VALUE);
            float minHumid = prefs.getFloat("min_humid", Float.MAX_VALUE);
            editor.putFloat("max_humid", Math.max(maxHumid, (float) humidity));
            editor.putFloat("min_humid", Math.min(minHumid, (float) humidity));
        }
        if (light != -999.0) {
            float maxLight = prefs.getFloat("max_light", Float.MIN_VALUE);
            float minLight = prefs.getFloat("min_light", Float.MAX_VALUE);
            editor.putFloat("max_light", Math.max(maxLight, (float) light));
            editor.putFloat("min_light", Math.min(minLight, (float) light));
        }
        if (pressure != -999.0) {
            float maxPressure = prefs.getFloat("max_pressure", Float.MIN_VALUE);
            float minPressure = prefs.getFloat("min_pressure", Float.MAX_VALUE);
            editor.putFloat("max_pressure", Math.max(maxPressure, (float) pressure));
            editor.putFloat("min_pressure", Math.min(minPressure, (float) pressure));
        }
        if (coPPM != -999.0) {
            float maxCo = prefs.getFloat("max_co", Float.MIN_VALUE);
            float minCo = prefs.getFloat("min_co", Float.MAX_VALUE);
            editor.putFloat("max_co", Math.max(maxCo, (float) coPPM));
            editor.putFloat("min_co", Math.min(minCo, (float) coPPM));
        }
        if (methanePPM != -999.0) {
            float maxMethane = prefs.getFloat("max_methane", Float.MIN_VALUE);
            float minMethane = prefs.getFloat("min_methane", Float.MAX_VALUE);
            editor.putFloat("max_methane", Math.max(maxMethane, (float) methanePPM));
            editor.putFloat("min_methane", Math.min(minMethane, (float) methanePPM));
        }
        if (smokePPM != -999.0) {
            float maxSmoke = prefs.getFloat("max_smoke", Float.MIN_VALUE);
            float minSmoke = prefs.getFloat("min_smoke", Float.MAX_VALUE);
            editor.putFloat("max_smoke", Math.max(maxSmoke, (float) smokePPM));
            editor.putFloat("min_smoke", Math.min(minSmoke, (float) smokePPM));
        }
        if (tvoc != -1) {
            int maxTvoc = prefs.getInt("max_tvoc", Integer.MIN_VALUE);
            int minTvoc = prefs.getInt("min_tvoc", Integer.MAX_VALUE);
            editor.putInt("max_tvoc", Math.max(maxTvoc, tvoc));
            editor.putInt("min_tvoc", Math.min(minTvoc, tvoc));
        }
        if (eco2 != -1) {
            int maxEco2 = prefs.getInt("max_eco2", Integer.MIN_VALUE);
            int minEco2 = prefs.getInt("min_eco2", Integer.MAX_VALUE);
            editor.putInt("max_eco2", Math.max(maxEco2, eco2));
            editor.putInt("min_eco2", Math.min(minEco2, eco2));
        }
        if (rainLevel != -999.0) {
            float maxRain = prefs.getFloat("max_rain", Float.MIN_VALUE);
            float minRain = prefs.getFloat("min_rain", Float.MAX_VALUE);
            editor.putFloat("max_rain", Math.max(maxRain, (float) rainLevel));
            editor.putFloat("min_rain", Math.min(minRain, (float) rainLevel));
        }

        // 保存 MQ 原始值
        editor.putInt("mq7_raw", mq7RawValue);
        editor.putInt("mq4_raw", mq4RawValue);
        editor.putInt("mq2_raw", mq2RawValue);

        editor.apply();
    }
    //显示通知
    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
    }
    //获取WIFI IP
    private String getWifiIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            // 将 ip 地址转换为字符串形式
            return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                    (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        }
        return null;
    }
    //网络状态更新函数
    private void updateNetworkStatus() {
        String ipAddress = getWifiIpAddress(); // 获取 IP 地址
        boolean wifiConnected = (ipAddress != null); // 通过 IP 地址判断 WiFi 连接状态
        //只有在 WiFi 连接时才检查 MQTT
        boolean mqttConnected = wifiConnected && (mqttClient != null && mqttClient.isConnected());
        runOnUiThread(() -> {
            // 更新 WiFi/IP 显示
            if (wifiIpTextView != null) {
                if (wifiConnected) {
                    wifiIpTextView.setText(getString(R.string.wifi_ip_prefix) + " " + ipAddress);
                } else {
                    wifiIpTextView.setText(R.string.wifi_disconnected);
                }
            }
            // 更新 EMQX 状态显示
            if (emqxStatusTextView != null) {
                if (mqttConnected) { //只有 WiFi 和 MQTT 都连接才显示地址
                    emqxStatusTextView.setText(getString(R.string.emqx_connected_prefix) + EMQX_BROKER_ADDRESS_FOR_DISPLAY);
                } else {
                    emqxStatusTextView.setText(R.string.emqx_disconnected);
                }
            }
        });
    }
    //处理 MQTT 消息
    private void handleMqttMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), "UTF-8");
            Log.d(TAG, "Message arrived on topic [" + topic + "]: " + payload);
            if (MQTT_TOPIC.equals(topic)) {
                // 处理传感器数据
                updateSensorData(payload);
            } else if (MQTT_EVENT_TOPIC.equals(topic)) {
                // 处理事件通知
                handleEventNotification(payload);
            }
        } catch (Exception e) {
            Log.e(TAG, "处理MQTT消息时出错", e);
        }
    }
    // 处理事件通知
    private void handleEventNotification(String payload) {
        try {
            JSONObject jsonObject = new JSONObject(payload);
            String eventType = jsonObject.optString("event");
            String eventId = jsonObject.optString("eventId", "");
            String notificationTitle = "";
            String notificationText = "";
            int notificationId = 0;
            String eventDescriptionForHistory = ""; // 用于存储到历史记录的描述

            if ("visitor_bell".equals(eventType)) {
                Log.i(TAG, "Visitor bell event received");
                notificationTitle = getString(R.string.notification_visitor_title);
                notificationText = getString(R.string.notification_visitor_text);
                notificationId = NOTIFICATION_ID_VISITOR;
                eventDescriptionForHistory = getString(R.string.event_desc_visitor_bell);
            } else if ("alarm".equals(eventType)) {
                String alarmTypeRaw = jsonObject.optString("type", getString(R.string.notification_alarm_unknown_type_raw)); // 获取原始报警类型
                double alarmValue = jsonObject.optDouble("value", -1);
                notificationTitle = getString(R.string.notification_alarm_title);
                // 根据 alarmTypeRaw 获取本地化的报警类型描述
                String displayAlarmType;
                switch (alarmTypeRaw) {
                    case "co":
                        displayAlarmType = getString(R.string.alarm_type_co);
                        break;
                    case "smoke_ppm":
                    case "smoke_raw":
                        displayAlarmType = getString(R.string.alarm_type_smoke);
                        break;
                    case "methane":
                    case "methane_ppm":
                        displayAlarmType = getString(R.string.alarm_type_methane);
                        break;
                    default://未知类型报警
                        displayAlarmType = getString(R.string.notification_alarm_unknown_type_display, alarmTypeRaw);
                }
                eventDescriptionForHistory = displayAlarmType + getString(R.string.alarm_suffix_history);
                StringBuilder notificationTextBuilder = new StringBuilder();
                notificationTextBuilder.append(getString(R.string.notification_alarm_text_prefix));
                notificationTextBuilder.append(displayAlarmType);
                notificationTextBuilder.append(getString(R.string.notification_alarm_text_suffix));

                if (alarmValue != -1) {
                    notificationTextBuilder.append(getString(R.string.notification_alarm_current_value));
                    // 根据报警类型决定小数位数
                    if ("co".equals(alarmTypeRaw) || "smoke_ppm".equals(alarmTypeRaw) || "methane_ppm".equals(alarmTypeRaw) || "methane".equals(alarmTypeRaw) ) {
                        notificationTextBuilder.append(String.format(Locale.getDefault(), "%.1f", alarmValue));
                    } else {
                        notificationTextBuilder.append(String.format(Locale.getDefault(), "%d", (int)alarmValue));
                    }
                }
                notificationText = notificationTextBuilder.toString();
                // 为不同的报警类型分配不同的通知ID
                if ("co".equals(alarmTypeRaw)) {
                    notificationId = NOTIFICATION_ID_ALARM_CO;
                } else if ("smoke_ppm".equals(alarmTypeRaw) || "smoke_raw".equals(alarmTypeRaw)) {
                    notificationId = NOTIFICATION_ID_ALARM_SMOKE;
                } else if ("methane".equals(alarmTypeRaw) || "methane_ppm".equals(alarmTypeRaw)) {
                    notificationId = NOTIFICATION_ID_ALARM_METHANE;
                } else {
                    notificationId = NOTIFICATION_ID_ALARM_GENERIC; // 通用报警ID
                }
            } else {
                Log.w(TAG, "Unknown event type received: " + eventType);
                return; // 未知事件类型，不处理也不保存
            }
            if (!notificationTitle.isEmpty()) {
                showNotification(notificationTitle, notificationText, notificationId);
            }
            // 将 eventId 传递给 saveEventToHistory
            if (!eventDescriptionForHistory.isEmpty()) {
                saveEventToHistory(eventDescriptionForHistory, System.currentTimeMillis(), eventId);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing event JSON: " + payload, e);
            showToast(getString(R.string.error_parsing_event_json));
        } catch (Exception e) {
            Log.e(TAG, "Error handling event notification: " + payload, e);
        }
    }
    //显示 Android 通知
    private void showNotification(String title, String content, int notificationId) {
        // 创建一个 Intent，用于在点击通知时打开 MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // 使用 PendingIntent.FLAG_IMMUTABLE 或 PendingIntent.FLAG_UPDATE_CURRENT
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_announcement_24) // 通知图标
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // 设置高优先级以弹出显示
                .setContentIntent(pendingIntent) //设置点击通知的操作
                .setAutoCancel(true); //点击通知后自动移除
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted. Cannot show notification.");
                return;
            }
        }
        // notificationId用于唯一标识这个通知。
        notificationManager.notify(notificationId, builder.build());
    }
    //网络状态更新定时器
    private void startNetworkUpdateTimer() {
        // 先移除旧的回调，防止重复启动
        wifiUpdateHandler.removeCallbacksAndMessages(null);
        wifiUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateNetworkStatus(); // 定时更新网络状态
                wifiUpdateHandler.postDelayed(this, WIFI_UPDATE_INTERVAL); // 再次设置定时器
            }
        }, WIFI_UPDATE_INTERVAL);
    }
    // 定时更新 Wi-Fi 信息
    private void startWifiUpdateTimer() {
        wifiUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateNetworkStatus();
                wifiUpdateHandler.postDelayed(this, WIFI_UPDATE_INTERVAL);
            }
        }, WIFI_UPDATE_INTERVAL);
    }
    //保存事件到 SharedPreferences
    private void saveEventToHistory(String eventDescription, long timestamp, String eventId) {
        //添加日志打印 eventId
        Log.d(TAG, "saveEventToHistory - Desc: " + eventDescription + " - eventId: " + eventId + " - Time: " + System.currentTimeMillis());

        if (eventId == null || eventId.isEmpty()) {
            Log.w(TAG, "Event ID is null or empty in saveEventToHistory for: " + eventDescription);
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        //事件 ID 去重
        if (eventId != null && !eventId.isEmpty()) {
            Set<String> processedEventIds = prefs.getStringSet(PREFS_PROCESSED_EVENT_IDS_KEY, new HashSet<>());
            if (processedEventIds.contains(eventId)) {
                Log.i(TAG, "Duplicate event (ID: " + eventId + ") detected by processed ID set, skipping save: " + eventDescription);
                return; // 重复事件，直接返回
            }
            // 如果不重复，则将此 eventId 添加到已处理列表
            Set<String> updatedProcessedIds = new HashSet<>(processedEventIds);
            updatedProcessedIds.add(eventId);
            // 保持已处理 ID 列表的大小
            if (updatedProcessedIds.size() > MAX_PROCESSED_IDS_HISTORY * 2) {
                List<String> tempList = new ArrayList<>(updatedProcessedIds);
                if (tempList.size() > MAX_PROCESSED_IDS_HISTORY) {
                    updatedProcessedIds = new HashSet<>(tempList.subList(tempList.size() - MAX_PROCESSED_IDS_HISTORY, tempList.size()));
                }
            }
            editor.putStringSet(PREFS_PROCESSED_EVENT_IDS_KEY, updatedProcessedIds);
        }

        Set<String> eventsSet = prefs.getStringSet(PREFS_EVENTS_KEY, new HashSet<>());
        List<String> eventsList = new ArrayList<>(eventsSet);

        JSONObject eventJson = new JSONObject();
        try {
            eventJson.put("timestamp", timestamp);
            eventJson.put("description", eventDescription);
            if (eventId != null && !eventId.isEmpty()) {
                eventJson.put("eventId", eventId);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error creating event JSON for history", e);
            return;
        }
        String newEventJsonString = eventJson.toString();

        eventsList.add(0, newEventJsonString);
        while (eventsList.size() > MAX_EVENT_HISTORY) {
            eventsList.remove(eventsList.size() - 1);
        }
        editor.putStringSet(PREFS_EVENTS_KEY, new HashSet<>(eventsList));
        editor.apply();
        Log.d(TAG, "Event saved to history: " + eventDescription + (eventId != null ? " (ID: " + eventId + ")" : ""));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy - isFinishing: " + isFinishing());
        wifiUpdateHandler.removeCallbacksAndMessages(null);
        if (mqttClient != null) {
            try {
                if (mqttClient.isConnected()) {
                    Log.i(TAG, "Disconnecting MQTT client in onDestroy...");
                    // 在断开前取消订阅，避免重连后自动订阅不期望的主题
                    mqttClient.disconnectForcibly(1000, 1000); // 强制断开，设置超时
                }
                mqttClient.close(); // 关闭客户端，释放资源
                Log.i(TAG, "MQTT client closed in onDestroy.");
            } catch (MqttException e) {
                Log.e(TAG, "Error disconnecting/closing MQTT client in onDestroy", e);
            }
            mqttClient = null;
        }
    }
}