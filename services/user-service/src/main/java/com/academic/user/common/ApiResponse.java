package com.academic.user.common;

import java.time.Instant;

public class ApiResponse {
    private int code; // 1 成功，0 或其他 失败
    private String msg;
    private Object data;
    private long timestamp;

    public ApiResponse() {}

    public ApiResponse(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public static  ApiResponse success(String msg, Object data) {
        return new ApiResponse(1, msg, data);
    }

    public static  ApiResponse fail(String msg) {
        return new ApiResponse(0, msg, "");
    }
    public static  ApiResponse fail(int code, String msg)
    {
        return new ApiResponse(code, msg, "");
    }

    // getters / setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}