package com.example.demo.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 封装需要在页面或者交给前端的各种数据
 */
@Data //lombok自动生成get set tostring等方法
@Accessors(chain = true) //能进行链式操作  所有的set方法的返回值就变成了R对象本身（原本是void）
public class R {
    private Integer code;//响应码
    private String message;//响应消息
    private Map<String,Object> data = new HashMap<>();

    public static R ok(){
        R r = new R();
        r.setCode(0);
        r.setMessage("成功");
        return r;
    }

    public static R error(){
        R r = new R();
        r.setCode(-1);
        r.setMessage("失败");
        return r;
    }

    public R data(String key, Object o){
        this.data.put(key,o);
        return this;
    }

}
