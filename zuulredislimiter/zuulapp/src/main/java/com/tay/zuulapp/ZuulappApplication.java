package com.tay.zuulapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;



@SpringBootApplication
@EnableZuulProxy
public class ZuulappApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZuulappApplication.class, args);
    }

}

