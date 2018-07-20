package com.jgb.loancalculator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

public class CounterServiceRedis implements CounterService {
    
    private static final String REDIS_KEY = "loan-calculator";

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;
    
    @Override
    public long incrementCounter() {
        return redisTemplate.opsForValue().increment(REDIS_KEY, 1);
    }

    @Override
    public void resetCount() {
        redisTemplate.opsForValue().set(REDIS_KEY, 5000);
    }
}
