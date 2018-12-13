

[English](https://github.com/tangaiyun/zuul-redislimiter-spring-boot/blob/master/README.md) | [中文](https://github.com/tangaiyun/zuul-redislimiter-spring-boot/blob/master/README-CN.md)

# zuul-redislimiter-spring-boot
rate limiter for zuul 

## Quickstart

### Clone, build and install
``` bash
git clone https://github.com/tangaiyun/zuul-redislimiter-spring-boot.git
cd zuulredislimiter/zuul-redislimiter-spring-boot-starter
mvn clean install
```

### Add to your POM
Then create a zuul proxy project refer to sample project "zuulredislimiter/zuulapp"，and you need to add dependency in pom.xml
``` java
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

```

``` xml
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tay</groupId>
            <artifactId>zuul-redislimiter-spring-boot-starter</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
    </dependencies>
```

### Configuration

For `resources/application.yml` you need to add the following lines.

``` yaml
server:
  port: 8000  

spring:
  application:
    name: service-zuul

zuul:
  routes:
    service1: /s1/**
    service2: /s2/**
  redis-limiter:
    redis-host: 127.0.0.1
    policy-map:
      api-a:
        order: -1
        baseExp: Headers['userid']
        pathRegExp: /s1/.*
        timeUnit: MINUTES
        permits: 2
      api-a1:
        order: 0
        baseExp: Headers['userid']
        pathRegExp: /s1.*
        timeUnit: MINUTES
        permits: 3
      api-b:
        order: 2
        baseExp: Headers['userid']
        pathRegExp: /s2/.*
        timeUnit: MINUTES
        permits: 5
eureka:
  client:
    serviceUrl:
          defaultZone: http://localhost:8761/eureka/
  instance:
      prefer-ip-address: true

```

### Create a sample microservice refer to zuulredislimiter/service1

``` java
package com.tay.service1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo1")
public class Controller1 {
    @GetMapping("/test11")
    public String test1() {
        return "test11!";
    }

    @GetMapping("/test12")
    public String test2() {
        return "test12!";
    }
}
```
``` yaml
server:
  port: 8001
spring:
  application:
    name: service1

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
``` 

### Create another sample microservice like zuulredislimiter/service2
``` java
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


``` yaml
server:
  port: 8002
spring:
  application:
    name: service2

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
``` 
### Create an Eureka server project like zuulredislimiter/eurekaserver
``` java
package com.tay.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class EurekaserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaserverApplication.class, args);
    }

}
```
``` yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

### Start Redis server

Start Redis server on a local machine or with Docker.

``` bash
sudo docker run -d -p 6379:6379 redis
```
### start eurekaserver project
### start zuulapp project
### start service1 project
### start service2 project

### Testing


You can use a HTTP client tool such as Postman, restd or curl, and get the URL `http://localhost:8000/s2/demo2/test21`. Don't forget to add a pair value `userid=tom` in your HTTP request header. You will be able to find the user with userid "tom" can visit this url five times successfully at most in one minute.

## Advanced Guide
### Complete Configuration Items

``` yaml

spring:
    redis-limiter: 
        redis-host: 127.0.0.1           # redis server IP                   default：127.0.0.1
        redis-port: 6379                # redis service port                default：6379  
        redis-password: test            # redis password                    default：null 
        redis-connection-timeout: 2000  # redis connection timeout          default：2000
        redis-pool-max-idle: 50         # redis pool max idle               default: 50
        redis-pool-min-idle: 10         # redis pool mim idle               default：10 
        redis-pool-max-wait-millis： -1 # max wait time for get connection  default：-1 
        redis-pool-max-total: 200       # max total connection              default：200
        redis-key-prefix: #RL           # key prefix for visit footprint    default: #RL
        check-action-timeout: 100       # check action execution timeout    default: 100
        channel： #RLConfigChannel      # conf change event pub/sub channel default： #RLConfigChannel
        policy-map:                     # rate limiting policies 
          api-a:                        # unique service id
            order: -1                   # order
            baseExp: Headers['userid']  # what base on to limit, Spel expression, support Headers['xxx'] or Cookies['xxx']  
            pathRegExp: /s1/.*          # URI path pattern, a Regular expression 
            timeUnit: MINUTES           # timeUnit supports SECONDS, MINUTES, HOURS,DAYS
            permits: 2                  # Number of visits allowed per a timeUnit
          api-a1:              
            order: 0
            baseExp: Headers['userid']
            pathRegExp: /s1.*
            timeUnit: MINUTES
            permits: 3
          ...  
```

### Dynamic configuration

We can change the configuraton by internal RESTful API.

``` java

@RequestMapping("/zuullimiterpolicy")
public class LimitingPolicyResource {
    ...

    @PostMapping
    public void add(@RequestBody LimitingPolicy limitingPolicy, HttpServletResponse response) throws IOException{
        ...
    }

    @PutMapping
    public void update(@RequestBody LimitingPolicy limitingPolicy, HttpServletResponse response) throws IOException {

        ...
    }
    @GetMapping("/{serviceId}")
    public LimitingPolicy get(@PathVariable("serviceId") String serviceId) {
        ...
    }

    @DeleteMapping("/{serviceId}")
    public void delete(@PathVariable("serviceId") String serviceId) {
        ...
    }

```

Currently, this framework support four actions (add, update, query, delete).

For example (as in the zuulapp project)

The  result will of GET http://localhost:8000/zuullimiterpolicy/api-a

``` json
{
  "serviceId": "api-a",
  "order": -1,
  "baseExp": "Headers['userid']",
  "pathRegExp": "/s1/.*",
  "timeUnit": "MINUTES",
  "permits": 2,
  "delete": false
}
```

If we want to update configuration, assign Content-Type as application/json, then excute PUT http://localhost:8000/zuullimiterpolicy, the request body as below: 

``` json
{
  "serviceId": "api-a",
  "order": -1,
  "baseExp": "Headers['userid']",
  "pathRegExp": "/s1/.*",
  "timeUnit": "MINUTES",
  "permits": 10,
  "delete": false
}
```
then the limiting policy for service with serviceId "api-a" changed.


If we want to delete a limiting policy with serviceId "api-b", execute DELETE http://localhost:8000/zuullimiterpolicy/api-b, the limiting policy with serviceId "api-b" will be deleted.

add a new limiting policy, assign Content-Type as application/json, then excute POST http://localhost:8000/zuullimiterpolicy, the request body as below: 

``` json
{
  "serviceId": "api-d",
  "order": -1,
  "baseExp": "Headers['userid']",
  "pathRegExp": "/s3/.*",
  "timeUnit": "MINUTES",
  "permits": 10,
  "delete": false
}
```
please be careful, the serviceId and pathRegExp should not be conflict with existed limiting policy.


