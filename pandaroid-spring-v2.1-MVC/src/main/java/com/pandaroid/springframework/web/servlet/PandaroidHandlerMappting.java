package com.pandaroid.springframework.web.servlet;

import java.lang.reflect.Method;

/**
 * @author pandaroid
 */
public class PandaroidHandlerMappting {
    /**
     * url ，后续考虑正则匹配
     */
    private String url;
    /**
     * method ，调用的 controller 相应的方法
     */
    private Method method;
    /**
     * method 对应的实例对象
     */
    private Object controller;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

}
