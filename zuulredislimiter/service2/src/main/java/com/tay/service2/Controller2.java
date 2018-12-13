package com.tay.service2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo2")
public class Controller2 {
    @GetMapping("/test21")
    public String test1() {
        return "test21!";
    }

    @GetMapping("/test22")
    public String test2() {
        return "test22!";
    }
}
