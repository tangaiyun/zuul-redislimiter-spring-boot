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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public final class LimitingPolicyManager extends JedisPubSub implements InitializingBean {
    private static Logger logger = LoggerFactory.getLogger(LimitingPolicyManager.class);

    private final ZuulRedisLimiterProperties zuulRedisLimiterProperties;

    private final PolicyValidator policyValidator;

    private ConcurrentHashMap<String, LimitingPolicy> policyMap = new ConcurrentHashMap<>();

    private Cache<String, LimitingPolicy> LimitingPolicyCache =
            Caffeine.newBuilder().maximumSize(10000).expireAfterAccess(1, TimeUnit.HOURS).build();

    @Override
    public void afterPropertiesSet() {
        SubThread subThread = new SubThread();
        subThread.start();
        WatcherThread watcherThread = new WatcherThread(subThread);
        watcherThread.start();

    }

    public void init() {
        Map<String, LimitingPolicy> policyConf = zuulRedisLimiterProperties.getPolicyMap();
        HashSet<String> pathSet = new HashSet<>();
        for (Map.Entry<String, LimitingPolicy> entry : policyConf.entrySet()) {
            String serviceId = entry.getKey();
            LimitingPolicy limitingPolicy = entry.getValue();
            limitingPolicy.setServiceId(serviceId);
            if(!policyValidator.validate(limitingPolicy)) {
                throw new RuntimeException("Zuul Limiting policy validate failed, the policy is " + limitingPolicy);
            }
            if (policyMap.containsKey(serviceId)) {
                throw new RuntimeException(String.format("Zuul Limiting policy includes duplicate serviceId %s", serviceId));
            }
            String pathRegExp = limitingPolicy.getPathRegExp();
            if (!pathSet.add(limitingPolicy.getPathRegExp())) {
                throw new RuntimeException(String.format("Zuul Limiting policy includes duplicate path regular expression %s", pathRegExp));
            }
            policyMap.put(serviceId, limitingPolicy);
        }
    }

    class SubThread extends Thread {
        boolean mistaken = false;
        @Override
        public void run() {
            Jedis jedis = null;
            try {
                jedis = new Jedis(zuulRedisLimiterProperties.getRedisHost(), zuulRedisLimiterProperties.getRedisPort(), 0);
                jedis.subscribe(LimitingPolicyManager.this, zuulRedisLimiterProperties.getChannel());
            }
            catch (JedisConnectionException e) {
                mistaken = true;
            }
            finally {
                if(jedis != null) {
                    jedis.close();
                }

            }
        }
        public boolean isMistaken() {
            return mistaken;
        }
    }

    class WatcherThread extends Thread {
        SubThread subThread;
        WatcherThread(SubThread subThread) {
            this.subThread = subThread;
        }
        public void run() {
            while(true) {
                try {
                    sleep(5000);
                }
                catch(InterruptedException e) {
                }
                if(subThread.isMistaken()) {
                    subThread = new SubThread();
                    subThread.start();
                }
            }
        }
    }


    @Override
    public void onMessage(String channel, String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        LimitingPolicy policy = null;
        try {
            policy = objectMapper.readValue(message, LimitingPolicy.class);
        }
        catch(IOException e) {
            logger.error("read config from message failed. the message content is " + message);
        }
        if(policy != null) {
            if (policy.isDelete()) {
                policyMap.remove(policy.getServiceId());
                LimitingPolicyCache.invalidateAll();
            } else {
                policyMap.put(policy.getServiceId(), policy);
            }
        }
    }

    public LimitingPolicy get(String serviceId) {
        LimitingPolicy limitingPolicy = policyMap.get(serviceId);
        //protect inner state, only return copy
        if(limitingPolicy != null) {
            return  limitingPolicy.copy();
        }
        return null;
    }

    public boolean containServiceId(String serviceId) {
        return policyMap.containsKey(serviceId);
    }

    public boolean containsPath(String pathExp) {
        for (LimitingPolicy limitingPolicy : policyMap.values()) {
            if(limitingPolicy.getPathRegExp().equals(pathExp)) {
                return true;
            }
        }
        return false;
    }

    public LimitingPolicy match(String requestPath){
        LimitingPolicy limitingPolicy = LimitingPolicyCache.getIfPresent(requestPath);
        if(limitingPolicy == null) {
            Optional<LimitingPolicy> target =  policyMap.values().stream().filter(p -> Pattern.matches(p.getPathRegExp(), requestPath)).sorted(Comparator.comparing(LimitingPolicy::getOrder)).findFirst();
            if(target.isPresent()) {
                limitingPolicy =  target.get();
                LimitingPolicyCache.put(requestPath, limitingPolicy);
            }
        }
        //protect inner state, only return copy
        if(limitingPolicy !=null) {
            return limitingPolicy.copy();
        }
        else
            return null;
    }

}
