package com.pandaroid.springframework.web.servlet;

import java.io.File;

/**
 * 对应具体 Template  页面
 * @author pandaroid
 */
public class PandaroidView {

    private File viewFile;

    public PandaroidView(File templateFile) {
        this.viewFile = templateFile;
    }

}
