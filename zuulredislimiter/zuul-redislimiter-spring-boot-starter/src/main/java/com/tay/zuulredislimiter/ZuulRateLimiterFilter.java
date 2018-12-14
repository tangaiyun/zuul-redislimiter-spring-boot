/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * @author  Aiyun Tang
 * @mail aiyun.tang@gmail.com
 */
package com.tay.zuulredislimiter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.tay.zuulredislimiter.core.RateCheckTaskRunner;
import com.tay.zuulredislimiter.event.RateExceedingEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public final class ZuulRateLimiterFilter extends ZuulFilter implements ApplicationContextAware {

    Logger logger = LoggerFactory.getLogger(ZuulRateLimiterFilter.class);
    private final ZuulRedisLimiterProperties zuulRedisLimiterProperties;
    private final LimitingPolicyManager limitingPolicyManager;
    private final RateCheckTaskRunner rateCheckTaskRunner;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
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
    public Object run() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest httpServletRequest = requestContext.getRequest();
        String requestPath = httpServletRequest.getRequestURI();
        LimitingPolicy limitingPolicy = limitingPolicyManager.match(requestPath);
        if(limitingPolicy != null) {
            String pathExp = limitingPolicy.getPathRegExp();
            TimeUnit timeUnit = TimeUnit.valueOf(limitingPolicy.getTimeUnit());
            int permits = limitingPolicy.getPermits();
            String baseExp = "#"+limitingPolicy.getBaseExp().trim();
            String baseVal = "";
            if(!"".equals(baseExp)) {
                baseVal = eval(baseExp, httpServletRequest);
            }
            String rateLimiterKey = zuulRedisLimiterProperties.getRedisKeyPrefix() + ":" + pathExp + ":" + baseVal;
            boolean isSuccess = rateCheckTaskRunner.checkRun(rateLimiterKey, timeUnit, permits);

            if(!isSuccess) {
                rateExceeded(requestContext, limitingPolicy, baseVal, requestPath);
            }

        }

        return null;
    }

    private String eval(String baseExp, HttpServletRequest request) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        ExpressionParser expressionParser = new SpelExpressionParser();
        mountCookies(request, context);
        mountHeaders(request, context);
        Expression expression = expressionParser.parseExpression(baseExp);
        String baseVal = expression.getValue(context, String.class);
        if(baseVal == null) {
            baseVal = "";
        }
        return baseVal;
    }

    private void mountCookies(HttpServletRequest request, StandardEvaluationContext context) {
        HashMap<String, String> cookieMap = new HashMap<>();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookieMap.put(cookie.getName(), cookie.getValue());
            }
        }
        context.setVariable("Cookies", cookieMap);
    }

    private void mountHeaders(HttpServletRequest request, StandardEvaluationContext context) {
        HashMap<String, String> headerMap = new HashMap();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headerMap.put(headerName, request.getHeader(headerName));
            }
        }
        context.setVariable("Headers", headerMap);
    }

    private void rateExceeded(RequestContext requestContext, LimitingPolicy limitingPolicy, String baseVal, String requestPath) {
        buildDenyResponse(requestContext);
        RateExceedingEvent rateExceedingEvent = new RateExceedingEvent();
        rateExceedingEvent.setServiceId(limitingPolicy.getServiceId());
        rateExceedingEvent.setBaseExp(limitingPolicy.getBaseExp());
        rateExceedingEvent.setBaseValue(baseVal);
        rateExceedingEvent.setPathExp(limitingPolicy.getPathRegExp());
        rateExceedingEvent.setPathValue(requestPath);
        rateExceedingEvent.setPermits(limitingPolicy.getPermits());
        rateExceedingEvent.setTimeUnit(limitingPolicy.getTimeUnit());
        applicationContext.publishEvent(rateExceedingEvent);
    }

    private void buildDenyResponse(RequestContext requestContext) {
        requestContext.setSendZuulResponse(false);
        requestContext.setResponseStatusCode(HttpStatus.FORBIDDEN.value());
        requestContext.setResponseBody("Access denied because of exceeding access rate!");
        requestContext.getResponse().setContentType("text/plain;charset=UTF-8");
    }
}
