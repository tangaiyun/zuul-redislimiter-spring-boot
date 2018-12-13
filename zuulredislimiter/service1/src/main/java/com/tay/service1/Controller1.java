package com.tay.service1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo1")
public class Controller1 {
    @GetMapping("/test11")
    public String test1() {
        return "test11!";
    }

    @GetMapping("/test12")
    public String test2() {
        return "test12!";
    }
}
