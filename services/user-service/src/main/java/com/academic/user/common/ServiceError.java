package com.academic.user.common;

//前端数据相关的错误
public class ServiceError extends Exception
{
    private String msg;
    private int code;
    public ServiceError(String message, int code)
    {
        this.msg = message;
        this.code = code;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    public int getCode()
    {
        return code;
    }

    public void setCode(int code)
    {
        this.code = code;
    }
}
