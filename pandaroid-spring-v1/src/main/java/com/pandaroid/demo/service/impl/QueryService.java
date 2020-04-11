package com.pandaroid.demo.service.impl;

import com.pandaroid.demo.service.IQueryService;
import com.pandaroid.springframework.annotation.PandaroidService;

import java.text.SimpleDateFormat;
import java.util.Date;

@PandaroidService
public class QueryService implements IQueryService {
    @Override
    public String query(String name) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(new Date());
        String json = "{name:\"" + name + "\",time:\"" + time + "\",thread:\"" + Thread.currentThread() + "\"}";
        System.out.println("这是在业务方法中打印的：" + json);
        return json;
    }
}
