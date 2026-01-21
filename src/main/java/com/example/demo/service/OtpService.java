package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;

    // store: email -> otp (expires)
    public void saveOtp(String email, String otp, Duration ttl) {
        redisTemplate.opsForValue().set(email, otp, ttl);
    }

    public String getOtp(String email) {
        return redisTemplate.opsForValue().get(email);
    }

    public void deleteOtp(String email) {
        redisTemplate.delete(email);
    }
}
