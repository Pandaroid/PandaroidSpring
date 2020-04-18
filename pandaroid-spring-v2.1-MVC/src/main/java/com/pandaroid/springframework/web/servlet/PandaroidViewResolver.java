package com.pandaroid.springframework.web.servlet;

import java.io.File;
import java.net.URL;

/**
 * 将页面 Template 变为 View 对象
 * @author pandaroid
 */
public class PandaroidViewResolver {

    private final String DEFAULT_TEMPLATE_SUFFIX = ".html";
    private File templateRootDir;

    public PandaroidViewResolver(String templateRoot) {
        URL templateRootUrl = this.getClass().getClassLoader().getResource(templateRoot);
        if(null == templateRootUrl) {
            return ;
        }
        templateRootDir = new File(templateRootUrl.getFile());
    }

    public PandaroidView resolveViewName(String viewName) {
        if(null == viewName || viewName.trim().isEmpty()) {
            return null;
        }
        viewName = viewName.trim();
        // viewName 不带后缀的话，添加后缀
        if(!viewName.endsWith(DEFAULT_TEMPLATE_SUFFIX)) {
            viewName += DEFAULT_TEMPLATE_SUFFIX;
        }
        // View 的构造函数参数 File
        String templateFilePath = templateRootDir.getPath() + File.separator + viewName;
        templateFilePath = templateFilePath.replaceAll("/+", "/");
        File templateFile = new File(templateFilePath);
        return new PandaroidView(templateFile);
    }

}
