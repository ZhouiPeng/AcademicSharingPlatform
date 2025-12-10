package com.academic.user.common;

import com.alibaba.fastjson2.JSON;

import java.time.Instant;

public class ApiResponse {
    private int code; // 1 成功，0 或其他 失败
    private String msg;
    private String data;
    private long timestamp;

    public ApiResponse() {}

    public ApiResponse(int code, String msg, String data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = Instant.now().toEpochMilli();
    }

//    public static  String success(String data) {
//        return new ApiResponse(1, "操作成功", data).toString();
//    }

    public static  String success(String msg, String data) {
        return new ApiResponse(1, msg, data).toString();
    }

    public static  String fail(String msg) {
        return new ApiResponse(0, msg, "").toString();
    }
    public static  String fail(int code, String msg)
    {
        return new ApiResponse(code, msg, "").toString();
    }

    // getters / setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString()
    {
        return JSON.toJSONString(this);
    }
}