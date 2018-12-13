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

import lombok.Data;

import java.util.concurrent.TimeUnit;
@Data
public final class LimitingPolicy {
    /**
     * unique service id
     */
    private String serviceId = "";
    /**
     * the order of policy
     */
    private int order = 100;
    /**
     * base Spel expression
     */
    private String baseExp = "";
    /**
     * path regExp
     */
    private String pathRegExp = "";
    /**
     * time unit for statistic
     */
    private String timeUnit = TimeUnit.SECONDS.name();
    /**
     * the allowed visiting count
     */
    private int permits = 10000;
    /**
     * the flag to tell whether this policy should be deleted
     */
    private boolean isDelete = false;

    public LimitingPolicy copy() {
        LimitingPolicy limitingPolicy = new LimitingPolicy();
        limitingPolicy.setServiceId(this.serviceId);
        limitingPolicy.setBaseExp(this.baseExp);
        limitingPolicy.setPathRegExp(this.pathRegExp);
        limitingPolicy.setTimeUnit(this.timeUnit);
        limitingPolicy.setPermits(this.permits);
        limitingPolicy.setDelete(this.isDelete);
        return limitingPolicy;
    }

}
