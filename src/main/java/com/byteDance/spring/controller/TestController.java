package com.byteDance.spring.controller;

import com.byteDance.spring.annotation.BDAutowired;
import com.byteDance.spring.annotation.BDController;
import com.byteDance.spring.annotation.BDRequestMapping;
import com.byteDance.spring.annotation.BDRequestParam;
import com.byteDance.spring.service.TestService;

@BDController
@BDRequestMapping(value = "/test")
public class TestController {

    @BDAutowired
    TestService testService;

    @BDRequestMapping(value = "/v1")
    public String v1(
            @BDRequestParam(value = "name")
                    String name) {
        return testService.getName(name);
    }

}
