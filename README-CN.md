
[中文](https://github.com/tangaiyun/zuul-redislimiter-spring-boot/blob/master/README-CN.md) | [English](https://github.com/tangaiyun/zuul-redislimiter-spring-boot/blob/master/README.md) 

# zuul-redislimiter-spring-boot
基于Zuul的限流器 

## 快速开始

### 克隆, 编译，安装
``` bash
git clone https://github.com/tangaiyun/zuul-redislimiter-spring-boot.git
cd zuulredislimiter/zuul-redislimiter-spring-boot-starter
mvn clean install
```

### 新建一个Zuul项目
参考项目 "zuulredislimiter/zuulapp"创建一个zuul网关项目，在pom.xml文件里添加必须的依赖
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

### 配置

修改 `resources/application.yml` 加入以下配置

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
    redis-host: 127.0.0.1                             #Redis 服务器   
    policy-map:
      api-a:                                          #serviceId为api-a的限流规则 
        order: -1                                     #规则排序，但一个请求有多条规则匹配时，排序值最小的规则生效
        baseExp: Headers['userid']                    #基于HTTP header里key为“userid”的值聚合统计
        pathRegExp: /s1/.*                            #请求URI的匹配正则表达式 
        timeUnit: MINUTES                             #时间单位，支持SECONDS,MINUTES,HOURS,DAYS 
        permits: 2                                    #单位时间内可访问的次数
      api-a1:
        order: 0
        baseExp: Cookies['userid']
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

### 参考项目zuulredislimiter/service1创建一个简单微服务

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

### 参考项目zuulredislimiter/service2创建一个简单微服务
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
```

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
### 参考项目zuulredislimiter/eurekaserver，创建一个Eureka server项目负责服务注册
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

在本机启动Redis或者用Docker启动Redis，指令如下：

``` bash
sudo docker run -d -p 6379:6379 redis
```
### 启动Eureka server项目
### 启动zuul网关项目
### 启动微服务service1
### 启动微服务service2

### 测试


你可以用一个HTTP Client工具像Postman、Restd或者curl,用GET形式访问“http://localhost:8000/s2/demo2/test21”，记得在header里面塞入userid=tom，重复请求多次，你会发现1分钟内，你最多只能成功请求5次。
通过查看配置可以得知，应该是这条限流策略生效了, 因为URI为/s2/demo2/test21跟pathRegExp: /s2/.*：匹配。
``` yaml
api-b:
  order: 2
  baseExp: Headers['userid']
  pathRegExp: /s2/.*
  timeUnit: MINUTES
  permits: 5
```
## 高级指南
### 所有的配置项

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
            baseExp: Headers['userid']  # value to base on, Spel expression without "#", supports Headers['xxx'] or Cookies['xxx']  
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

### 动态配置

通过内置的Restful接口，我们可以动态的变更限流规则，当然这些API应该受限访问的，不过API安全性是另外一个故事，这里不展开.

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

目前，限流规则支持增删查改(add, update, query, delete).

比如，在示例项目里面

GET http://localhost:8000/zuullimiterpolicy/api-a，访问值如下：

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

更新限流规则，指定Content-Type为“application/json”, 请求 PUT http://localhost:8000/zuullimiterpolicy, 请求request body为: 

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



删除限流规则，规则的serviceId为"api-b"， 执行 DELETE http://localhost:8000/zuullimiterpolicy/api-b。

新增限流规则，指定Content-Type为“application/json”, 请求 POST http://localhost:8000/zuullimiterpolicy, 请求request body为: 

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
注意，新增限流规则时，新增规则的serviceId和pathRegExp不能跟已经存在的规则相同，不然新增失败。

