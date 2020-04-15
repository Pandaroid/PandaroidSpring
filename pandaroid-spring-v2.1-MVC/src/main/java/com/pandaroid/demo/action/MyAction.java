package com.pandaroid.demo.action;

import com.pandaroid.demo.service.IModifyService;
import com.pandaroid.demo.service.IQueryService;
import com.pandaroid.springframework.annotation.PandaroidAutowired;
import com.pandaroid.springframework.annotation.PandaroidController;
import com.pandaroid.springframework.annotation.PandaroidRequestMapping;
import com.pandaroid.springframework.annotation.PandaroidRequestParam;
import com.pandaroid.springframework.web.servlet.PandaroidModelAndView;

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
    public PandaroidModelAndView query(HttpServletRequest request, HttpServletResponse response,
                                       @PandaroidRequestParam("name") String name){
        String result = queryService.query(name);
        return out(response,result);
    }

    @PandaroidRequestMapping("/add*.json")
    public PandaroidModelAndView add(HttpServletRequest request,HttpServletResponse response,
                    @PandaroidRequestParam("name") String name,@PandaroidRequestParam("addr") String addr){
        String result = modifyService.add(name,addr);
        return out(response,result);
    }

    @PandaroidRequestMapping("/remove.json")
    public PandaroidModelAndView remove(HttpServletRequest request,HttpServletResponse response,
                       @PandaroidRequestParam("id") Integer id){
        String result = modifyService.remove(id);
        return out(response,result);
    }

    @PandaroidRequestMapping("/edit.json")
    public PandaroidModelAndView edit(HttpServletRequest request,HttpServletResponse response,
                     @PandaroidRequestParam("id") Integer id,
                     @PandaroidRequestParam("name") String name){
        String result = modifyService.edit(id,name);
        return out(response,result);
    }


    private PandaroidModelAndView out(HttpServletResponse resp, String str){
        try {
            resp.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
