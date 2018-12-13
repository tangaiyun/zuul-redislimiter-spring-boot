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

import com.tay.zuulredislimiter.core.RateCheckTaskRunner;
import com.tay.zuulredislimiter.core.RedisRateLimiterFactory;
import com.tay.zuulredislimiter.event.DefaultRateCheckFailureListener;
import com.tay.zuulredislimiter.event.DefaultRateExceedingListener;
import com.tay.zuulredislimiter.event.RateCheckFailureListener;
import com.tay.zuulredislimiter.event.RateExceedingListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


@Configuration
@EnableConfigurationProperties(ZuulRedisLimiterProperties.class)
public class ZuulRedisLimiterConfiguration {

    @Autowired
    private ZuulRedisLimiterProperties zuulRedisLimiterProperties;

    @Bean
    @ConditionalOnMissingBean(JedisPool.class)
    public JedisPool jedisPool() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(zuulRedisLimiterProperties.getRedisPoolMaxIdle());
        jedisPoolConfig.setMinIdle(zuulRedisLimiterProperties.getRedisPoolMinIdle());
        jedisPoolConfig.setMaxWaitMillis(zuulRedisLimiterProperties.getRedisPoolMaxWaitMillis());
        jedisPoolConfig.setMaxTotal(zuulRedisLimiterProperties.getRedisPoolMaxTotal());
        jedisPoolConfig.setTestOnBorrow(true);
        JedisPool jedisPool = new JedisPool(jedisPoolConfig, zuulRedisLimiterProperties.getRedisHost(), zuulRedisLimiterProperties.getRedisPort(), zuulRedisLimiterProperties.getRedisConnectionTimeout(), zuulRedisLimiterProperties.getRedisPassword());
        return jedisPool;
    }

    @Bean
    @ConditionalOnMissingBean(RedisRateLimiterFactory.class)
    public RedisRateLimiterFactory redisRateLimiterFactory() {
        RedisRateLimiterFactory redisRateLimiterFactory = new RedisRateLimiterFactory(jedisPool());
        return redisRateLimiterFactory;
    }


    @Bean
    @ConditionalOnMissingBean(RateCheckTaskRunner.class)
    public RateCheckTaskRunner rateCheckTaskRunner() {
        RateCheckTaskRunner rateCheckTaskRunner = new RateCheckTaskRunner(redisRateLimiterFactory(), zuulRedisLimiterProperties);
        return rateCheckTaskRunner;
    }

    @Bean
    @ConditionalOnMissingBean(RateCheckFailureListener.class)
    public RateCheckFailureListener rateCheckFailureListener() {
        RateCheckFailureListener rateCheckFailureListener = new DefaultRateCheckFailureListener();
        return rateCheckFailureListener;
    }

    @Bean
    @ConditionalOnMissingBean(RateExceedingListener.class)
    public RateExceedingListener rateExceedingListener() {
        RateExceedingListener rateExceedingListener = new DefaultRateExceedingListener();
        return rateExceedingListener;
    }

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean(LimitingPolicyManager.class)
    public LimitingPolicyManager limitingPolicyManager() {
        LimitingPolicyManager limitingPolicyManager = new LimitingPolicyManager(zuulRedisLimiterProperties);
        return limitingPolicyManager;
    }

    @Bean
    @ConditionalOnMissingBean(ZuulRateLimiterFilter.class)
    public ZuulRateLimiterFilter zuulRateLimiterFilter() {
        ZuulRateLimiterFilter zuulRateLimiterFilter = new ZuulRateLimiterFilter(zuulRedisLimiterProperties, limitingPolicyManager(), rateCheckTaskRunner());
        return zuulRateLimiterFilter;
    }

    @Bean
    @ConditionalOnMissingBean(LimitingPolicyResource.class)
    public LimitingPolicyResource limitingPolicyResource() {
        LimitingPolicyResource limitingPolicyResource = new LimitingPolicyResource(jedisPool(), zuulRedisLimiterProperties, limitingPolicyManager());
        return limitingPolicyResource;
    }
}
