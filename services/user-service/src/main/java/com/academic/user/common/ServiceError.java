package com.academic.user.common;

// 前端数据相关的错误
public class ServiceError extends Exception {

    private final int code;

    public ServiceError(String message, int code) {
        super(message);          // ⭐ 关键：把 message 交给父类
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return super.getMessage();
    }
}