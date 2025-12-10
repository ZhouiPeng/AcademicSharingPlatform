package com.academic.achievement.dto;

public class ApiResponseCollectionFolderDto {
    private int code;
    private String msg;
    private CollectionFolderDto data;
    private long timestamp;

    public ApiResponseCollectionFolderDto() {}

    public ApiResponseCollectionFolderDto(int code, String msg, CollectionFolderDto data, long timestamp) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = timestamp;
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

    public CollectionFolderDto getData() {
        return data;
    }

    public void setData(CollectionFolderDto data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
