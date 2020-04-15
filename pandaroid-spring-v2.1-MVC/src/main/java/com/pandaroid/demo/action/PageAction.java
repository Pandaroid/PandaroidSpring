package com.pandaroid.demo.action;

import com.pandaroid.demo.service.IQueryService;
import com.pandaroid.springframework.annotation.PandaroidAutowired;
import com.pandaroid.springframework.annotation.PandaroidController;
import com.pandaroid.springframework.annotation.PandaroidRequestMapping;
import com.pandaroid.springframework.annotation.PandaroidRequestParam;
import com.pandaroid.springframework.web.servlet.PandaroidModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * @author pandaroid
 */
@PandaroidController
@PandaroidRequestMapping("/page")
public class PageAction {
    @PandaroidAutowired
    IQueryService queryService;

    @PandaroidRequestMapping("/welcome.html")
    public PandaroidModelAndView query(@PandaroidRequestParam("nickname") String nickname) {
        String result = queryService.query(nickname);
        Map<String, Object> model = new HashMap<>();
        model.put("nickname", nickname);
        model.put("data", result);
        model.put("token", System.currentTimeMillis());
        return new PandaroidModelAndView("welcome.html", model);
    }
}
