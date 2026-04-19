package com.ticks.user_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTokenService {

    private static final String BLACKLIST_PREFIX = "blacklist:access:";
    private static final String LOGIN_ATTEMPTS_PREFIX = "login:attempts:";
    private static final String USER_CACHE_PREFIX = "user:profile:";
    private static final String RESET_TOKEN_PREFIX = "reset:token:";
    private static final String VERIFY_TOKEN_PREFIX = "verify:token:";

    private final RedisTemplate<String, String> redisTemplate;

    public void blackListAccessToken(String jti, long ttlMillis) {
        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "revoked", ttlMillis, TimeUnit.MILLISECONDS);
        log.debug("Access token blacklisted: {}", jti);
    }

    public boolean isAccessTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    // Increments the failed login counter for a given key

    public long incrementLoginAttempt(String key) {
        String redisKey = LOGIN_ATTEMPTS_PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(15));
        }
        return count == null ? 1 : count;
    }

    public long getLoginAttempts(String key) {
        String value = redisTemplate.opsForValue().get(LOGIN_ATTEMPTS_PREFIX + key);
        return value == null ? 0 : Long.parseLong(value);
    }

    public void resetLoginAttempts(String key) {
        redisTemplate.delete(LOGIN_ATTEMPTS_PREFIX + key);
    }

    // User profile cache

    public void cacheUserProfile(String email, String userJson) {
        redisTemplate.opsForValue().set(USER_CACHE_PREFIX + email, userJson, Duration.ofMinutes(30));
    }

    public String getCachedUserProfile(String email) {
        return redisTemplate.opsForValue().get(USER_CACHE_PREFIX + email);
    }

    public void evictUserProfile(String email) {
        redisTemplate.delete(USER_CACHE_PREFIX + email);
        log.debug("User profile cache evicted for: {}", email);
    }

    // Password reset token cache

    public void cacheResetToken(String token, String email) {
        redisTemplate.opsForValue().set(RESET_TOKEN_PREFIX + token, email, Duration.ofMinutes(30));
    }

    public String getResetTokenEmail(String token) {
        return redisTemplate.opsForValue().get(RESET_TOKEN_PREFIX + token);
    }

    public void evictResetToken(String token) {
        redisTemplate.delete(RESET_TOKEN_PREFIX + token);
    }

    // Email verification token cache

    public void cacheVerificationToken(String rawToken, String userId, LocalDateTime expiresAt) {

        long ttlSeconds = java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
        log.info("ttl seconds for cache verification token: {}", ttlSeconds);
        if (ttlSeconds <= 0) {
            log.warn("Skipping verification token cache — token already expired");
            return;
        }
        redisTemplate.opsForValue().set(VERIFY_TOKEN_PREFIX + rawToken, userId, ttlSeconds, TimeUnit.SECONDS);
        log.debug("Verification token cached for userId: {}", userId);
    }

    public String getVerificationTokenUserId(String rawToken) {
        return redisTemplate.opsForValue().get(VERIFY_TOKEN_PREFIX + rawToken);
    }

    public void evictVerificationToken(String rawToken) {
        redisTemplate.delete(VERIFY_TOKEN_PREFIX + rawToken);
        log.debug("Verification token evicted from cache");
    }


}
