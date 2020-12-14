package com.ofwiki.dm.rocketmq.dto;

/**
 * @author HuangJS
 * @date 2020-12-11 4:45 下午
 */
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public Result(){

    }

    private Result(T data){
        this.data = data;
    }

    public static <E> Result success(E e){
        return new Result(e);
    }
    public static <E> Result success(){
        return new Result();
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
}
