package com.example.projectaih;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ChatViewModel extends ViewModel {

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>()); // 消息列表
    private final MutableLiveData<String> statusText = new MutableLiveData<>("Ready"); // 状态文本
    private final MutableLiveData<Boolean> isSending = new MutableLiveData<>(false); // 是否正在发送消息
    private final MutableLiveData<List<String>> availableModels = new MutableLiveData<>(); // 可用模型列表
    private final OkHttpClient client; // OkHttp 客户端
    private final Gson gson = new Gson(); // Gson 实例，用于 JSON 处理
    // 保存与 API 交互的消息记录，用于构建请求体
    private final List<JsonObject> apiMessages = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // 用于后台任务的线程池
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // 用于在主线程上更新 UI

    public ChatViewModel() {
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // 设置连接超时
                .readTimeout(60, TimeUnit.SECONDS) // 设置读取超时
                .writeTimeout(60, TimeUnit.SECONDS) // 设置写入超时
                .build();
    }
    public LiveData<List<Message>> getMessages() {
        return messages;
    }
    public LiveData<String> getStatusText() {
        return statusText;
    }
    public LiveData<Boolean> getIsSending() {
        return isSending;
    }
    public LiveData<List<String>> getAvailableModels() {
        return availableModels;
    }
    // 加载可用模型列表
    public void loadModels(String baseUrl) {
        String url = baseUrl + "/api/tags"; // 构建请求 URL
        Request request = new Request.Builder().url(url).get().build(); // 创建 GET 请求
        client.newCall(request).enqueue(new Callback() { // 发送异步请求
            @Override
            public void onFailure(Call call, IOException e) {
                // 请求失败，更新状态文本
                mainHandler.post(() -> statusText.setValue("Error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 请求成功
                if (!response.isSuccessful()) {
                    // HTTP 错误，更新状态文本
                    mainHandler.post(() -> statusText.setValue("Error: HTTP " + response.code()));
                    return;
                }

                try (ResponseBody responseBody = response.body()) { // 获取响应体
                    if (responseBody == null) {
                        // 响应体为空，更新状态文本
                        mainHandler.post(() -> statusText.setValue("Error: Empty response body"));
                        return;
                    }
                    String responseBodyString = responseBody.string(); // 获取响应体字符串

                    JSONObject jsonResponse = new JSONObject(responseBodyString); // 解析 JSON
                    JSONArray modelsArray = jsonResponse.getJSONArray("models"); // 获取模型数组
                    List<String> modelNames = new ArrayList<>(); // 创建模型名称列表
                    for (int i = 0; i < modelsArray.length(); i++) {
                        modelNames.add(modelsArray.getJSONObject(i).getString("name")); // 提取模型名称
                    }
                    mainHandler.post(() -> availableModels.setValue(modelNames)); // 更新可用模型列表
                } catch (JSONException e) {
                    mainHandler.post(() -> statusText.setValue("Error parsing JSON: " + e.getMessage()));
                }
            }
        });
    }

    // 发送消息
    public void sendMessage(String baseUrl, String model, String prompt, boolean stream) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return; // 提示为空，直接返回
        }

        isSending.postValue(true); // 设置正在发送消息标志
        statusText.postValue("Sending request..."); // 更新状态文本
        addMessage(new Message("You: " + prompt, true)); // 添加用户消息到 UI
        apiMessages.add(createMessageObject("user", prompt)); // 添加用户消息到 API 消息列表

        JSONObject requestBodyJson = new JSONObject(); // 创建请求体 JSON 对象
        try {
            requestBodyJson.put("model", model); // 设置模型名称
            requestBodyJson.put("stream", stream); // 设置是否流式传输
            // 将 List<JsonObject> 转换为 JSONArray
            JSONArray jsonArray = new JSONArray();
            for (JsonObject msg : apiMessages) {
                jsonArray.put(new JSONObject(gson.toJson(msg))); // 将每个 JsonObject 转换为 JSONObject
            }
            requestBodyJson.put("messages", jsonArray); // 设置消息列表
        } catch (JSONException e) {
            mainHandler.post(() -> statusText.setValue("JSON Error during body creation" + e.getMessage()));
            return; // JSON 错误，直接返回
        }

        RequestBody requestBody = RequestBody.create(requestBodyJson.toString(), MediaType.get("application/json")); // 创建请求体
        Request request = new Request.Builder()
                .url(baseUrl + "/api/chat") // 设置请求 URL
                .post(requestBody) // 设置请求方法为 POST
                .build(); // 构建请求

        client.newCall(request).enqueue(new Callback() { // 发送异步请求
            @Override
            public void onFailure(Call call, IOException e) {
                // 请求失败，更新 UI
                mainHandler.post(() -> {
                    addMessage(new Message("Error: " + e.getMessage(), false, true)); // 添加错误消息
                    isSending.setValue(false); // 重置正在发送消息标志
                    statusText.setValue("Ready"); // 重置状态文本
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 请求成功
                if (!response.isSuccessful()) {
                    // HTTP 错误，更新 UI
                    mainHandler.post(() -> {
                        addMessage(new Message("Error: HTTP " + response.code(), false, true)); // 添加错误消息
                        isSending.setValue(false); // 重置正在发送消息标志
                        statusText.setValue("Ready"); // 重置状态文本
                    });
                    return;
                }
                // 处理流式和非流式响应
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    // 响应体为空，更新 UI
                    mainHandler.post(() -> {
                        addMessage(new Message("Error: Empty response body", false, true)); // 添加错误消息
                        isSending.setValue(false); // 重置正在发送消息标志
                        statusText.setValue("Ready"); // 重置状态文本
                    });
                    return;
                }

                if (stream) {
                    handleStreamingResponse(responseBody); // 处理流式响应
                } else {
                    handleNonStreamingResponse(responseBody); // 处理非流式响应
                }
            }
        });

    }


    // 处理流式响应
    private void handleStreamingResponse(ResponseBody responseBody) {
        try {
            InputStreamReader reader = new InputStreamReader(responseBody.byteStream()); // 创建 InputStreamReader
            JsonStreamParser parser = new JsonStreamParser(reader); // 创建 JsonStreamParser

            mainHandler.post(() -> statusText.setValue("Receiving response...")); // 更新状态文本

            StringBuilder currentAIResponse = new StringBuilder(); // 用于存储当前 AI 响应的 StringBuilder
            while (parser.hasNext()) { // 循环处理每个 JSON 元素
                try {
                    JsonElement element = parser.next(); // 获取下一个 JSON 元素
                    if (element.isJsonObject()) { // 如果是 JSON 对象
                        JsonObject jsonObject = element.getAsJsonObject();

                        if (jsonObject.has("message")) { // 如果包含 "message" 字段
                            JsonObject message = jsonObject.getAsJsonObject("message"); // 获取 "message" 对象
                            if (message.has("content")) { // 如果 "message" 对象包含 "content" 字段
                                String chunk = message.get("content").getAsString(); // 获取内容块
                                currentAIResponse.append(chunk); // 将内容块追加到 currentAIResponse
                                // 使用当前流式内容更新 UI
                                mainHandler.post(() -> updateStreamingResponse(currentAIResponse.toString()));
                            }
                        }

                        if (jsonObject.has("done") && jsonObject.get("done").getAsBoolean()) { // 如果包含 "done" 字段且为 true
                            String role = jsonObject.getAsJsonObject("message").get("role").getAsString(); // 获取角色
                            // 完整的响应已经在 currentAIResponse 中
                            String content = currentAIResponse.toString();

                            if ("assistant".equals(role)) { // 如果角色是 "assistant"
                                // 我们不在这里添加消息；updateStreamingResponse 会处理它
                                apiMessages.add(createMessageObject("assistant", content)); // 将完整的 AI 响应添加到 API 消息列表
                            }
                            break; // 处理完 "done" 标志后退出循环
                        }
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> addMessage(new Message("Error parsing JSON: " + e.getMessage(), false, true)));
                    break;
                }
            }
            reader.close(); // 关闭 InputStreamReader
        } catch (IOException e) {
            mainHandler.post(() -> addMessage(new Message("Error reading stream: " + e.getMessage(), false, true)));
        } finally {
            responseBody.close(); // 关闭 ResponseBody
            mainHandler.post(() -> {
                isSending.setValue(false); // 重置正在发送消息标志
                statusText.setValue("Ready"); // 重置状态文本
            });
        }
    }


    // 处理非流式响应
    private void handleNonStreamingResponse(ResponseBody responseBody) {
        try {
            String responseBodyString = responseBody.string(); // 获取响应体字符串
            JSONObject jsonResponse = new JSONObject(responseBodyString); // 解析 JSON

            if (jsonResponse.has("message")) { // 如果包含 "message" 字段
                JSONObject message = jsonResponse.getJSONObject("message"); // 获取 "message" 对象
                String role = message.getString("role"); // 获取角色
                String content = message.getString("content"); // 获取内容

                if ("assistant".equals(role)) { // 如果角色是 "assistant"
                    mainHandler.post(() -> {
                        addMessage(new Message("AI: " + content, false)); // 添加 AI 消息到 UI
                        apiMessages.add(createMessageObject("assistant", content)); // 将 AI 消息添加到 API 消息列表
                    });
                }
            }
        } catch (JSONException | IOException e) {
            mainHandler.post(() -> addMessage(new Message("Error parsing JSON: " + e.getMessage(), false, true)));
        } finally {
            responseBody.close(); // 关闭 ResponseBody
            mainHandler.post(() -> {
                isSending.setValue(false); // 重置正在发送消息标志
                statusText.setValue("Ready"); // 重置状态文本
            });
        }
    }


    // 添加消息到 UI
    public void addMessage(Message message) {
        List<Message> currentMessages = messages.getValue(); // 获取当前消息列表
        if (currentMessages == null) {
            currentMessages = new ArrayList<>();
        }
        ArrayList<Message> updatedMessages = new ArrayList<>(currentMessages); // 创建消息列表的副本
        updatedMessages.add(message); // 添加新消息
        messages.postValue(updatedMessages);  // 更新消息列表
    }

    // 清除对话
    public void clearConversation() {
        apiMessages.clear(); // 清空 API 消息列表
        messages.postValue(new ArrayList<>()); // 清空 UI 消息列表
    }

    // 创建 API 消息对象
    private JsonObject createMessageObject(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    // 更新 UI 以显示流式响应 (传入当前响应)
    private void updateStreamingResponse(String currentResponse) {
        List<Message> currentMessages = messages.getValue(); // 获取当前消息列表
        if (currentMessages == null) currentMessages = new ArrayList<>();

        ArrayList<Message> updatedMessages = new ArrayList<>(currentMessages); // 创建消息列表的副本

        // 如果存在最后一个 AI 消息且不是用户消息，则移除它
        int lastIndex = updatedMessages.size() - 1;
        if (lastIndex >= 0 && !updatedMessages.get(lastIndex).isUser()) {
            updatedMessages.remove(lastIndex);
        }
        // 添加新消息
        updatedMessages.add(new Message("AI: " + currentResponse, false)); // 使用当前响应内容
        messages.postValue(updatedMessages); // 更新消息列表
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown(); // 关闭线程池，防止内存泄漏
    }
}