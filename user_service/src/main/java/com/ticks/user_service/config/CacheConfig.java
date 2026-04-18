package com.ticks.user_service.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.cache.default-ttl-minutes:10}")
    private long defaultTtlMinutes;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(defaultTtlMinutes))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues()
                .prefixCacheNameWith("user-svc::");

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(

                // GET /users/me - called on every authenticated request
                CacheNames.USER_BY_EMAIL,
                defaults.entryTtl(Duration.ofMinutes(10)),

                // GET /users/{id} - admin lookups, changes less often
                CacheNames.USER_BY_ID,
                defaults.entryTtl(Duration.ofMinutes(30)),

                // Registration duplicate check
                CacheNames.USER_EXISTS_BY_EMAIL,
                defaults.entryTtl(Duration.ofMinutes(5)),

                CacheNames.USERS_EXISTS_BY_USERNAME,
                defaults.entryTtl(Duration.ofMinutes(5))
        );

        return RedisCacheManager.builder()
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
