package com.trendyol.qualitymeasurementsample.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("test")
public class TestController {

    private static final String TEST_MESSAGE = "Hello Sonar!";

    @GetMapping
    public String getTestMessage() {
        // BUG crítico
        String s = null;
        System.out.println(s.length()); // NullPointerException
        
        return TEST_MESSAGE;
    }

}
