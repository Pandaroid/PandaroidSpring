package com.pandaroid.demo.service.impl.same;

import com.pandaroid.demo.service.ISameModifyService;
import com.pandaroid.springframework.annotation.PandaroidService;

@PandaroidService("sameModifyService")
public class ModifyService implements ISameModifyService {

    @Override
    public String add(String name, String addr) {
        return "modifyService add,name=" + name + ",addr=" + addr;
    }

    @Override
    public String edit(Integer id, String name) {
        return "modifyService edit,id=" + id + ",name=" + name;
    }

    @Override
    public String remove(Integer id) {
        return "modifyService id=" + id;
    }

}
