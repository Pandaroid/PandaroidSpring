package com.pandaroid.demo.service.impl.same;

import com.pandaroid.demo.service.IModifyService;
import com.pandaroid.springframework.annotation.PandaroidService;

@PandaroidService("sameModifyService")
public class ModifyService implements IModifyService {

    public String add(String name,String addr) {
        return "modifyService add,name=" + name + ",addr=" + addr;
    }

    public String edit(Integer id,String name) {
        return "modifyService edit,id=" + id + ",name=" + name;
    }

    public String remove(Integer id) {
        return "modifyService id=" + id;
    }

}
