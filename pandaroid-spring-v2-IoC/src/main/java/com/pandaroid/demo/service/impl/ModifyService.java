package com.pandaroid.demo.service.impl;

import com.pandaroid.demo.service.IModifyService;
import com.pandaroid.springframework.annotation.PandaroidService;

@PandaroidService
public class ModifyService implements IModifyService {

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
        return "modifyService remove id=" + id;
    }

}
