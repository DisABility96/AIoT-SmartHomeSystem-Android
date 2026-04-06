package com.example.projectaih;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    private EditText urlField;
    private MaterialAutoCompleteTextView modelAutoCompleteTextView; // 使用 MaterialAutoCompleteTextView
    private TextView statusLabel;
    private ProgressBar progressBar;
    private RecyclerView conversationRecyclerView;
    private EditText promptArea;
    private Button sendButton;
    private CheckBox streamCheckBox;
    private Button clearButton;
    private MessageAdapter messageAdapter;
    private ChatViewModel viewModel;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 初始化视图
        urlField = findViewById(R.id.urlField);
        modelAutoCompleteTextView = findViewById(R.id.modelAutoCompleteTextView);
        statusLabel = findViewById(R.id.statusLabel);
        progressBar = findViewById(R.id.progressBar);
        conversationRecyclerView = findViewById(R.id.conversationRecyclerView);
        promptArea = findViewById(R.id.promptArea);
        sendButton = findViewById(R.id.sendButton);
        streamCheckBox = findViewById(R.id.streamCheckBox);
        clearButton = findViewById(R.id.clearButton);
        // 设置 RecyclerView
        messageAdapter = new MessageAdapter();
        conversationRecyclerView.setAdapter(messageAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        conversationRecyclerView.setLayoutManager(layoutManager);
        // 获取 ViewModel
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        // 观察 LiveData ，消息、状态、是否发送
        viewModel.getMessages().observe(this, new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messages) {
                messageAdapter.submitList(new ArrayList<>(messages));
                if (((LinearLayoutManager) conversationRecyclerView.getLayoutManager()).findLastVisibleItemPosition() == messageAdapter.getItemCount() - 2) {
                    conversationRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                }
            }
        });

        viewModel.getStatusText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String status) {
                statusLabel.setText(status);
            }
        });

        viewModel.getIsSending().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isSending) {
                progressBar.setVisibility(isSending ? View.VISIBLE : View.GONE);
                sendButton.setEnabled(!isSending);
            }
        });

        // 观察可用模型列表
        viewModel.getAvailableModels().observe(this, models -> {
            // 使用 ArrayAdapter 填充 AutoCompleteTextView
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, models);
            modelAutoCompleteTextView.setAdapter(adapter);
            if (!models.isEmpty()) {
                modelAutoCompleteTextView.setText(models.get(0), false); // 预选第一个模型
            }
        });

        // 设置点击监听器
        sendButton.setOnClickListener(v -> sendMessage());
        clearButton.setOnClickListener(v -> viewModel.clearConversation());
        promptArea.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // 加载模型列表
        viewModel.loadModels(urlField.getText().toString());
    }

    private void sendMessage() {
        String url = urlField.getText().toString().trim();
        String model = modelAutoCompleteTextView.getText().toString().trim(); // 获取选中的模型
        Editable promptEditable = promptArea.getText();
        String prompt = promptEditable.toString().trim();
        boolean stream = streamCheckBox.isChecked();

        if (!prompt.isEmpty()) {
            viewModel.sendMessage(url, model, prompt, stream);
            promptEditable.clear();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(promptArea.getWindowToken(), 0);

        } else {
            Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}