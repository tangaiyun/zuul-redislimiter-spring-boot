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
package com.tay.zuulredislimiter.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

@Data
public final class RateExceedingEvent extends ApplicationEvent {
    private static Object dummy = new Object();
    /**
     * service id(unique)
     */
    private String serviceId;
    /**
     * Spel expression, support getting value from http header and cookie
     */
    private String baseExp;
    /**
     * value when the Spel expression was evaluated
     */
    private String baseValue;
    /**
     * the path regular expression
     */
    private String pathExp;
    /**
     * the real request path which matchs the pattern
     */
    private String pathValue;
    /**
     * time unit fo statistic period
     */
    private String timeUnit;
    /**
     * permits during a time unit
     */
    private int permits;
    public RateExceedingEvent() {
        super(dummy);
    }
}
