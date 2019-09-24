package com.byteDance.spring.bean;

import java.lang.reflect.Method;
import java.util.Map;

public class Handlermapping {

    //request中去除项目路径的uri
    private String url;

    //controller
    private Object controller;

    //执行的方法
    private Method method;

    //参数顺序
    private Map<String, Integer> paramSortMap;

    //参数列表
    private Class<?>[] parameterTypes;

    public String getUrl() {
        return url;
    }

    public Handlermapping setUrl(String url) {
        this.url = url;
        return this;
    }

    public Object getController() {
        return controller;
    }

    public Handlermapping setController(Object controller) {
        this.controller = controller;
        return this;
    }

    public Method getMethod() {
        return method;
    }

    public Handlermapping setMethod(Method method) {
        this.method = method;
        return this;
    }

    public Map<String, Integer> getParamSortMap() {
        return paramSortMap;
    }

    public Handlermapping setParamSortMap(Map<String, Integer> paramSortMap) {
        this.paramSortMap = paramSortMap;
        return this;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Handlermapping setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
        return this;
    }
}
