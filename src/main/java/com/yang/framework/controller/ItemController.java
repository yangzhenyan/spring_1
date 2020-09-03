package com.yang.framework.controller;

import com.yang.framework.annotation.YAutowired;
import com.yang.framework.annotation.YController;
import com.yang.framework.annotation.YRequestMapping;
import com.yang.framework.annotation.YRequestParam;
import com.yang.framework.service.ItemService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author yzy
 * @date 2020/8/26
 * @describe
 */
@YController
@YRequestMapping("item")
public class ItemController {
    @YAutowired
    private ItemService itemService;

    private ItemService itemService1;

    //查询
    @YRequestMapping("query")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @YRequestParam("name") String name, @YRequestParam("id") String id) {
        try {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("Amos的名字叫做：" + name + "      id为：" + id);
        } catch (IOException e) {
            e.getCause().getMessage();
        }
    }

    //添加
    @YRequestMapping("add")
    public void add(HttpServletRequest request, HttpServletResponse response, @YRequestParam("name") String name) {
        try {
            response.getWriter().write("增加name：" + name);
        } catch (IOException e) {
            e.getCause().getMessage();
        }
    }

    //删除
    @YRequestMapping("remove")
    public void remove(HttpServletRequest request, HttpServletResponse response, @YRequestParam("name") String name) {
        System.out.println("remove方法");
    }
}
