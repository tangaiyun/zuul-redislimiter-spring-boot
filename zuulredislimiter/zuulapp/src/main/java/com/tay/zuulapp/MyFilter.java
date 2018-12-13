package com.tay.zuulapp;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class MyFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest httpServletRequest = requestContext.getRequest();
        String requestPath = httpServletRequest.getRequestURI();
        String requestURL = httpServletRequest.getRequestURL().toString();
        System.out.println(requestPath);
        System.out.println(requestURL);
        System.out.println(httpServletRequest.getRemoteHost());
        return null;
    }
}
