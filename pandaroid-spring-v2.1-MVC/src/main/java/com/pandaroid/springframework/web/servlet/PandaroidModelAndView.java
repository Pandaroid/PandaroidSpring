package com.pandaroid.springframework.web.servlet;

import java.util.Map;

/**
 * @author pandaroid
 */
public class PandaroidModelAndView {
    /**
     * 视图名：表示要返回到哪个页面（视图）
     */
    private String viewName;
    /**
     * 视图 model 数据
     */
    private Map<String, ?> model;

    /**
     * 不需要数据的页面
     * @param viewName
     */
    public PandaroidModelAndView(String viewName) {
        this.viewName = viewName;
    }

    /**
     * 需要数据的页面
     * @param viewName
     * @param model
     */
    public PandaroidModelAndView(String viewName, Map<String, ?> model) {
        this.viewName = viewName;
        this.model = model;
    }

    public String getViewName() {
        return viewName;
    }

    public Map<String, ?> getModel() {
        return model;
    }
}
