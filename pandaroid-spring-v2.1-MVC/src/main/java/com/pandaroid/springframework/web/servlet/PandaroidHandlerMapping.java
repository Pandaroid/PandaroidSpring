package com.pandaroid.springframework.web.servlet;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author pandaroid
 */
public class PandaroidHandlerMapping {
    /**
     * url ，后续考虑正则匹配
     */
    private Pattern pattern;
    /**
     * method ，调用的 controller 相应的方法
     */
    private Method method;
    /**
     * method 对应的实例对象
     */
    private Object controller;

    public PandaroidHandlerMapping(Pattern pattern, Object controller, Method method) {
        this.pattern = pattern;
        this.controller = controller;
        this.method = method;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PandaroidHandlerMapping that = (PandaroidHandlerMapping) o;
        return pattern.equals(that.pattern) &&
                method.equals(that.method) &&
                controller.equals(that.controller);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, method, controller);
    }
}
