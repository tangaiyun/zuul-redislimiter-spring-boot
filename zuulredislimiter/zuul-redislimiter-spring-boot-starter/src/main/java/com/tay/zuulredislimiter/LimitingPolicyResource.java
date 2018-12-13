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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/zuullimiterpolicy")
public class LimitingPolicyResource {
    Logger logger = LoggerFactory.getLogger(LimitingPolicyResource.class);

    private final JedisPool jedisPool;

    private final ZuulRedisLimiterProperties zuulRedisLimiterProperties;

    private final LimitingPolicyManager limitingPolicyManager;

    @PostMapping
    public void add(@RequestBody LimitingPolicy limitingPolicy, HttpServletResponse response) throws IOException{
        String serviceId = limitingPolicy.getServiceId();
        String pathExp = limitingPolicy.getPathRegExp();
        if(limitingPolicyManager.containServiceId(serviceId)) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().print(String.format("Can not create a LimitingPolicy, because serviceId - %s is existed.", serviceId));
            return;
        }
        if(limitingPolicyManager.containsPath(pathExp)) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().print(String.format("Can not create a LimitingPolicy, because pathExp - %s is existed.", pathExp));
            return;
        }
        publish(limitingPolicy);
    }

    @PutMapping
    public void update(@RequestBody LimitingPolicy limitingPolicy, HttpServletResponse response) throws IOException {

        if(limitingPolicyManager.containServiceId(limitingPolicy.getServiceId()) && limitingPolicyManager.containsPath(limitingPolicy.getPathRegExp())) {
            publish(limitingPolicy);
        }
        else {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().print("Bad request for updating limiter configuration!");
        }
    }
    @GetMapping("/{serviceId}")
    public LimitingPolicy get(@PathVariable("serviceId") String serviceId) {
        return limitingPolicyManager.get(serviceId);
    }

    @DeleteMapping("/{serviceId}")
    public void delete(@PathVariable("serviceId") String serviceId) {
        LimitingPolicy limitingPolicy = limitingPolicyManager.get(serviceId);
        if(limitingPolicy != null) {
            limitingPolicy.setDelete(true);
            publish(limitingPolicy);
        }
    }

    private void publish(LimitingPolicy limitingPolicy) {
        ObjectMapper objectMapper = new ObjectMapper();
        String policyMessage = null;
        try {
            policyMessage = objectMapper.writeValueAsString(limitingPolicy);
        }
        catch(IOException e) {
            logger.error("convert LimiterConfig object to json failed.");
        }
        Jedis jedis = jedisPool.getResource();
        jedis.publish(zuulRedisLimiterProperties.getChannel(), policyMessage);
    }
}
