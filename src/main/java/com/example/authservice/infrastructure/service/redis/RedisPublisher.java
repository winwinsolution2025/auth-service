package com.example.authservice.infrastructure.service.redis;

import com.example.authservice.domain.gateway.publisher.MessagePublisher;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisPublisher implements MessagePublisher {
    private final JedisPool jedisPool;

    public RedisPublisher(String host, int port, String password) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);

        int timeout = 2000;
        if (password == null || password.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, host, port, timeout);
        } else {
            jedisPool = new JedisPool(poolConfig, host, port, timeout, password);
        }
    }

    @Override
    public void publish(String subject, String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(subject, message);
        } catch (Exception e) {
            throw new RuntimeException("failed to publish to redis subject", e);
        }
    }
}
