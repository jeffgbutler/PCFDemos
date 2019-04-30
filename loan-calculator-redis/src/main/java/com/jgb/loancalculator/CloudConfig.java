package com.jgb.loancalculator;

import com.jgb.loancalculator.service.CounterService;
import com.jgb.loancalculator.service.CounterServiceRedis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;

@Configuration
@Profile("cloud")
public class CloudConfig {

    @Bean
    public RedisTemplate<String, Integer> redisTemplate(RedisConnectionFactory redisFactory) {
        RedisTemplate<String, Integer> template = new RedisTemplate<>();
        template.setConnectionFactory(redisFactory);
        template.setValueSerializer(new GenericToStringSerializer<>(Integer.class));
        return template;
    }
    
    @Bean
    public CounterService counterService() {
        return new CounterServiceRedis();
    }
}
