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

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class DefaultPolicyValidator implements PolicyValidator {
	private static String[] TIMEUNITS = {TimeUnit.SECONDS.name(), TimeUnit.MINUTES.name(), TimeUnit.HOURS.name(), TimeUnit.DAYS.name()};
	private static HashSet<String> validTimeUnitNames = new HashSet<String>(Arrays.asList(TIMEUNITS));
    @Override
    public boolean validate(LimitingPolicy limitingPolicy) {
        boolean isValid = false;
        if(limitingPolicy != null) {
        	isValid = checkBaseExp(limitingPolicy.getBaseExp()) && checkTimeUnit(limitingPolicy.getTimeUnit()) && checkPermits(limitingPolicy.getPermits());
        }
        return isValid;
    }
    
    private boolean checkBaseExp(String baseExp) {
    	if(baseExp != null) {
    		if(baseExp.startsWith("Headers['") && baseExp.endsWith("']")) {
    			return true;
    		}
    		if(baseExp.startsWith("Cookies['") && baseExp.endsWith("']")) {
    			return true;
    		}
    	}
    	return false;
    }
    
    private boolean checkTimeUnit(String timeUnit) {
    	return validTimeUnitNames.contains(timeUnit);
    }
    
    private boolean checkPermits(int permits) {
    	return permits > 0;
    }
}
