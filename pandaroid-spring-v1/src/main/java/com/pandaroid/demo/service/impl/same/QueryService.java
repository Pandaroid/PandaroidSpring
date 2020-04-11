package com.pandaroid.demo.service.impl.same;

import com.pandaroid.demo.service.ISameQueryService;
import com.pandaroid.springframework.annotation.PandaroidService;

import java.text.SimpleDateFormat;
import java.util.Date;

@PandaroidService("sameQueryService")
public class QueryService implements ISameQueryService {
    @Override
    public String query(String name) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(new Date());
        String json = "{name:\"" + name + "\",time:\"" + time + "\"}";
        System.out.println("这是在业务方法中打印的：" + json);
        return json;
    }
}
