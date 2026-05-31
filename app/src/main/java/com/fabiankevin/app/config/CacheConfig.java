package com.fabiankevin.app.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public Caffeine<Object, Object> defaultCaffeine() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .initialCapacity(100)
                .maximumSize(10_000)
                .recordStats();
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> defaultCaffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("categories", "accounts");

        cacheManager.setCaffeine(defaultCaffeine);
        return cacheManager;
    }
}