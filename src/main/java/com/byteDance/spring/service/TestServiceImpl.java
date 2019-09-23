package com.byteDance.spring.service;

import com.byteDance.spring.annotation.BDService;

@BDService
public class TestServiceImpl implements TestService {

    @Override
    public String getName(String name) {
        return name + "是妖怪";
    }

}
