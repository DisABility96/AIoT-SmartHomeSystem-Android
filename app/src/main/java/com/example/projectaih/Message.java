package com.example.projectaih;
public class Message {
    private String text;
    private boolean isUser;
    private boolean isError; // 添加 isError 字段

    // 构造函数 (包含 isError)
    public Message(String text, boolean isUser, boolean isError) {
        this.text = text;
        this.isUser = isUser;
        this.isError = isError;
    }

    // 构造函数 (不包含 isError，默认为 false)
    public Message(String text, boolean isUser) {
        this(text, isUser, false); // 调用上面的构造函数，并将 isError 设置为 false
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }

    public boolean isError() {
        return isError;
    }

    // 可以选择添加 setter 方法
    public void setError(boolean error) {
        isError = error;
    }
}