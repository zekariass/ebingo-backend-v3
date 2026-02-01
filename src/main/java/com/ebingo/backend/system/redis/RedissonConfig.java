//package com.ebingo.backend.system.redis;
//
//import org.redisson.Redisson;
//import org.redisson.api.RedissonClient;
//import org.redisson.api.RedissonReactiveClient;
//import org.redisson.config.Config;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RedissonConfig {
//
//    @Bean(destroyMethod = "shutdown")
//    public RedissonReactiveClient redissonReactiveClient() {
//        Config config = new Config();
//        config.useSingleServer()
//                .setAddress("redis://127.0.0.1:6379") // change if needed
//                .setConnectionMinimumIdleSize(1)
//                .setConnectionPoolSize(10);
//
//        RedissonClient client = Redisson.create(config);
//        return client.reactive();
//    }
//}

//package com.ebingo.backend.system.redis;

//
//@Configuration
//public class RedissonConfig {
//
//    @Bean(destroyMethod = "shutdown")
//    public RedissonReactiveClient redissonReactiveClient() {
//        Config config = new Config();
//        config.useSingleServer()
//                .setAddress("redis://localhost:6379") // change if needed
//                .setConnectionMinimumIdleSize(1)
//                .setConnectionPoolSize(10);
//
//        RedissonClient client = Redisson.create(config);
//        return client.reactive();
//    }
//}


package com.ebingo.backend.system.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${REDIS_HOST:redis-ebingo}") // default to Docker service name
    private String redisHost;

    @Value("${REDIS_PORT:6379}") // default Redis port
    private int redisPort;

    @Value("${REDIS_PASSWORD:}") // default empty if not set
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedissonReactiveClient redissonReactiveClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectionMinimumIdleSize(8)
                .setConnectionPoolSize(32)
                .setSubscriptionConnectionPoolSize(16)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(3000)
                .setTimeout(10000);

        RedissonClient client = Redisson.create(config);
        return client.reactive();
    }
}




