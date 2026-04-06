package com.example.projectaih;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class MessageAdapter extends ListAdapter<Message, MessageAdapter.MessageViewHolder> {

    public MessageAdapter() {
        super(DIFF_CALLBACK);
    }

    // DiffUtil 回调，用于比较消息列表中的差异
    private static final DiffUtil.ItemCallback<Message> DIFF_CALLBACK = new DiffUtil.ItemCallback<Message>() {
        @Override
        public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            // 对于真实的应用，你可能需要使用消息的唯一 ID 进行比较
            return oldItem == newItem;  // 这里暂时使用对象引用比较 (如果可能，请使用更好的比较方式)
        }

        @Override
        public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            // 比较消息内容和发送者是否相同
            return oldItem.getText().equals(newItem.getText()) && oldItem.isUser() == newItem.isUser();
        }
    };


    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 创建 ViewHolder
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        // 绑定数据到 ViewHolder
        Message message = getItem(position);
        holder.bind(message);
    }


    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageTextView; // 显示消息文本的 TextView
        private LinearLayout messageContainer; // 用于控制消息对齐的 LinearLayout (左对齐或右对齐)

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            messageContainer = itemView.findViewById(R.id.messageContainer); // 获取 LinearLayout
        }

        public void bind(Message message) {
            messageTextView.setText(message.getText()); // 设置消息文本

            if (message.isUser()) {
                // 用户消息：右对齐，设置背景和文本颜色
                messageTextView.setBackgroundResource(R.drawable.user_message_background);
                messageTextView.setTextColor(Color.WHITE);
                messageContainer.setGravity(Gravity.END); // 右对齐
            } else {
                // AI 消息：左对齐，设置背景和文本颜色
                messageTextView.setBackgroundResource(R.drawable.ai_message_background);
                messageTextView.setTextColor(Color.BLACK);
                messageContainer.setGravity(Gravity.START); // 左对齐
            }
        }
    }
}