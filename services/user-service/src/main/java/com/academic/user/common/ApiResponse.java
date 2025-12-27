package com.academic.user.common;

import java.time.Instant;

public class ApiResponse<T> {
    private int code; // 1 成功，0 或其他 失败
    private String msg;
    private T data;
    private long timestamp;

    public ApiResponse() {}

    public ApiResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public   ApiResponse<T> success(String msg, T data) {
        return new ApiResponse<> (1, msg, data);
    }

    public   ApiResponse<T> fail(String msg) {
        return new ApiResponse<>(0, msg, null);
    }
    public   ApiResponse<T> fail(int code, String msg)
    {
        return new ApiResponse<>(code, msg, null);
    }

    // getters / setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}