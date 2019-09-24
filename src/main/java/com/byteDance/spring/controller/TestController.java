package com.byteDance.spring.controller;

import com.byteDance.spring.annotation.BDAutowired;
import com.byteDance.spring.annotation.BDController;
import com.byteDance.spring.annotation.BDRequestMapping;
import com.byteDance.spring.annotation.BDRequestParam;
import com.byteDance.spring.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@BDController
@BDRequestMapping(value = "/test")
public class TestController {

    @BDAutowired
    TestService testService;

    @BDRequestMapping(value = "/v1")
    public String v1(
            @BDRequestParam(value = "name")
                    String name,
            HttpServletRequest request,
            HttpServletResponse response) {
        System.out.println(request);
        System.out.println(response);
        return testService.getName(name);
    }

}
