package com.yang.framework.service.impl;

import com.yang.framework.annotation.YService;
import com.yang.framework.service.ItemService;

/**
 * @author yzy
 * @date 2020/8/26
 * @describe
 */
@YService
public class ItemServiceImpl implements ItemService {

    public Object getBean(String name) {
        return "bean：" + name;
    }
}
