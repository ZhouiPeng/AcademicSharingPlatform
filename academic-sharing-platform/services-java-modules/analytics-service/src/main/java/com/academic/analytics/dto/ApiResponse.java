package com.academic.analytics.dto;

public class ApiResponse<T> {

    private int code = 1;
    private String msg = "操作成功";
    private T data;
    private long timestamp = System.currentTimeMillis();

    public ApiResponse() {
    }

    public ApiResponse(T data) {
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
