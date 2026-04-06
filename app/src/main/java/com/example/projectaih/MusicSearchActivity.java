package com.example.projectaih;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicSearchActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, SeekBar.OnSeekBarChangeListener{

    private static final String TAG = "MusicSearchActivity";
    private static final String JAMENDO_CLIENT_ID = "请自行申请Jamendo客户端ID";
    private static final String JAMENDO_API_URL = "https://api.jamendo.com/v3.0/tracks/?client_id=" + JAMENDO_CLIENT_ID + "&format=jsonpretty&limit=10&search=";

    private EditText searchEditText;
    private Button searchButton;
    private ListView resultsListView;
    private ArrayList<String> musicTitles;
    private ArrayList<String> musicArtists;
    private ArrayList<String> musicUrls;
    private ArrayList<String> musicImages;

    private ArrayAdapter<String> adapter;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private MediaPlayer mediaPlayer;
    private LinearLayout playbackControlLayout;
    private ImageView albumArtImageView;
    private SeekBar seekBar;
    private ImageButton playPauseButton;
    private ImageButton stopButton;
    private TextView timeView;
    private Handler handler = new Handler(); // 用于更新 UI
    // 标记当前是否正在播放
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_search);

        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        resultsListView = findViewById(R.id.resultsListView);

        musicTitles = new ArrayList<>();
        musicArtists = new ArrayList<>();
        musicUrls = new ArrayList<>(); // 如果需要
        musicImages = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, musicTitles);
        resultsListView.setAdapter(adapter);

        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString();
            if (!query.isEmpty()) {
                searchMusic(query);
            }
        });

        resultsListView.setOnItemClickListener((parent, view, position, id) -> {
            String url = musicUrls.get(position);
            playMusic(url);
            //新增：根据url加载封面
            if (musicImages != null && musicImages.size() > position) {
                loadAlbumArt(musicImages.get(position));
            }
        });

        //获取播放控制 UI 元素
        playbackControlLayout = findViewById(R.id.playbackControlLayout);
        albumArtImageView = findViewById(R.id.albumArtImageView);
        seekBar = findViewById(R.id.seekBar);
        playPauseButton = findViewById(R.id.playPauseButton);
        stopButton = findViewById(R.id.stopButton); //新增
        timeView = findViewById(R.id.time);

        seekBar.setOnSeekBarChangeListener(this); // 设置 SeekBar 监听器

        // 播放/暂停 按钮点击事件
        playPauseButton.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play); // 切换到播放图标
                    isPlaying = false;//
                } else {
                    mediaPlayer.start();
                    playPauseButton.setImageResource(android.R.drawable.ic_media_pause); // 切换到暂停图标
                    isPlaying = true; //
                }
            }
        });
        //停止按钮
        stopButton.setOnClickListener(v->{
            if(mediaPlayer!=null){
                mediaPlayer.stop();
                try {
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                isPlaying = false;
            }
        });
    }

    private void searchMusic(String query) {
        String apiUrl = JAMENDO_API_URL + query;

        executorService.execute(() -> { //在后台线程执行
            try {
                String responseString = httpGetRequest(apiUrl); // 发送 HTTP GET 请求
                if (responseString != null) {
                    parseJson(responseString); // 解析 JSON 响应
                } else {
                    runOnUiThread(() -> Toast.makeText(MusicSearchActivity.this, "Error: No response from server", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MusicSearchActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

    }
    //你需要实现一个httpGetRequest函数,可以使用HttpURLConnection
    private  String httpGetRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        //连接超时
        connection.setConnectTimeout(5000);
        //读取超时
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if(responseCode == HttpURLConnection.HTTP_OK){
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine())!=null){
                response.append(line);
            }
            reader.close();
            return response.toString();
        }else{
            Log.e(TAG,"HTTP GET request failed with response code: "+ responseCode);
            return null;
        }
    }

    private void parseJson(String responseString) {
        try {
            JSONObject jsonResponse = new JSONObject(responseString);
            JSONArray results = jsonResponse.getJSONArray("results");


            musicTitles.clear();
            musicArtists.clear();
            musicUrls.clear(); // 如果需要
            musicImages.clear();//新增

            for (int i = 0; i < results.length(); i++) {
                JSONObject track = results.getJSONObject(i);
                String name = track.getString("name");
                String artist = track.getString("artist_name");
                String url = track.getString("audio"); // 直接获取 "audio" 字段
                String image = track.getString("image");//新增

                musicTitles.add(name + " - " + artist);
                musicUrls.add(url); // 保存 URL
                musicImages.add(image);//新增
            }


            runOnUiThread(() -> {
                adapter.notifyDataSetChanged(); // 更新 ListView
                //新增：在列表的第一项加载专辑封面,搜索结束就更新封面
                if (!musicImages.isEmpty()) {
                    loadAlbumArt(musicImages.get(0));
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(MusicSearchActivity.this, "Error parsing JSON", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
        if (mediaPlayer != null) {
            mediaPlayer.release(); // 释放 MediaPlayer 资源
            mediaPlayer = null;
        }
    }

    //新增：播放音乐
    private void playMusic(String url) {
        Log.d(TAG, "playMusic called with URL: " + url);

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "MediaPlayer released");
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC); // 设置音频流类型
        mediaPlayer.setOnPreparedListener(this);       // 设置准备完成监听器
        mediaPlayer.setOnCompletionListener(this);     // 设置播放完成监听器
        mediaPlayer.setOnErrorListener(this);          // 设置错误监听器

        try {
            mediaPlayer.setDataSource(url); // 设置数据源 (URL)
            Log.d(TAG, "MediaPlayer setDataSource: " + url);
            mediaPlayer.prepareAsync(); // 异步准备
            Log.d(TAG, "MediaPlayer prepareAsync called");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error setting data source", e); // 修改错误信息
            showToast("Error setting data source: " + e.getMessage());
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // MediaPlayer 准备完成后开始播放
        Log.d(TAG, "MediaPlayer onPrepared");
        mp.start();
        isPlaying = true;
        showToast("Playing...");
        // 新增：显示播放控制 UI
        playbackControlLayout.setVisibility(View.VISIBLE);
        // 新增：设置 SeekBar 的最大值
        seekBar.setMax(mediaPlayer.getDuration());
        // 新增：开始更新 SeekBar
        startSeekBarUpdate();
        //更新按钮图标
        playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        //停止按钮
        stopButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // 播放完成后释放 MediaPlayer
        Log.d(TAG, "MediaPlayer onCompletion");
        showToast("播放完成"); //
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        //新增：播放结束，隐藏
        playbackControlLayout.setVisibility(View.GONE);
        //新增：重置播放状态
        isPlaying = false;
        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        stopButton.setVisibility(View.GONE);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // 播放出错
        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
        showToast("MediaPlayer error: what=" + what + ", extra=" + extra);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        //新增：隐藏
        playbackControlLayout.setVisibility(View.GONE);
        //新增：重置播放状态
        isPlaying = false;
        return true; // 返回 true 表示已处理错误
    }
    //这里需要用到MainActivity中的sendMqttMessage函数，但为了解耦，这里不做具体实现
//你需要自己实现
    private void sendMqttMessage(String topic, String message) {
        //请根据你的实际情况填写
    }

    private void showToast(final String message){
        runOnUiThread(() -> Toast.makeText(MusicSearchActivity.this,message,Toast.LENGTH_LONG).show());
    }

    //新增：更新seekbar
    private void startSeekBarUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);

                    // 更新播放时间
                    updatePlaybackTime(currentPosition, mediaPlayer.getDuration());

                    handler.postDelayed(this, 100); // 每 100 毫秒更新一次
                }
            }
        }, 100);
    }

    //新增: 实现 SeekBar.OnSeekBarChangeListener 接口的方法
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // 当用户拖动 SeekBar 时，更新播放时间显示，但先不跳转
        if (fromUser && mediaPlayer != null) {
            updatePlaybackTime(progress, mediaPlayer.getDuration());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // 用户开始触摸 SeekBar 时，先暂停更新
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // 用户停止触摸 SeekBar 时，跳转到指定位置
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(seekBar.getProgress()); //调整进度
        }
        // 重新开始更新 SeekBar
        startSeekBarUpdate();
    }
    //新增：更新播放时间显示
    private void updatePlaybackTime(int currentPosition, int duration) {
        String currentTime = formatTime(currentPosition);
        String totalTime = formatTime(duration);
        timeView.setText(String.format("%s/%s", currentTime, totalTime));
    }

    //新增：格式化时间
    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
    // 新增：加载并显示专辑封面 (需要添加依赖)
    private void loadAlbumArt(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()  // 使用 Picasso 库加载图片
                    .load(imageUrl)
                    .placeholder(R.drawable.baseline_music_note_24) // 占位图 (可选)
                    .error(R.drawable.baseline_error_24) // 错误图 (可选)
                    .into(albumArtImageView); // 显示到 ImageView
        } else {
            // 如果没有图片 URL，可以显示一个默认图片
            albumArtImageView.setImageResource(R.drawable.baseline_music_note_24);
        }
    }
}