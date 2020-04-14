package com.pandaroid.demo.action;

import com.pandaroid.demo.service.IModifyService;
import com.pandaroid.demo.service.IQueryService;
import com.pandaroid.springframework.annotation.PandaroidAutowired;
import com.pandaroid.springframework.annotation.PandaroidController;
import com.pandaroid.springframework.annotation.PandaroidRequestMapping;
import com.pandaroid.springframework.annotation.PandaroidRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@PandaroidController
@PandaroidRequestMapping("/web*")
public class MyAction {
    @PandaroidAutowired
    IModifyService modifyService;
    @PandaroidAutowired
    IQueryService queryService;

    @PandaroidRequestMapping("/query.json")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @PandaroidRequestParam("name") String name){
        String result = queryService.query(name);
        out(response,result);
    }

    @PandaroidRequestMapping("/add*.json")
    public void add(HttpServletRequest request,HttpServletResponse response,
                    @PandaroidRequestParam("name") String name,@PandaroidRequestParam("addr") String addr){
        String result = modifyService.add(name,addr);
        out(response,result);
    }

    @PandaroidRequestMapping("/remove.json")
    public void remove(HttpServletRequest request,HttpServletResponse response,
                       @PandaroidRequestParam("id") Integer id){
        String result = modifyService.remove(id);
        out(response,result);
    }

    @PandaroidRequestMapping("/edit.json")
    public void edit(HttpServletRequest request,HttpServletResponse response,
                     @PandaroidRequestParam("id") Integer id,
                     @PandaroidRequestParam("name") String name){
        String result = modifyService.edit(id,name);
        out(response,result);
    }


    private void out(HttpServletResponse resp, String str){
        try {
            resp.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
