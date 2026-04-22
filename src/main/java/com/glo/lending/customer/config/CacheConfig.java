package com.glo.lending.customer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;


@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String CACHE_CUSTOMERS = "customers";
    public static final String CACHE_LOAN_LIMITS = "customerLoanLimits";

    @Bean
    public CacheManager cacheManager() {
        log.info("Initializing CaffeineCacheManager with TTL");

        CaffeineCacheManager manager = new CaffeineCacheManager(
                CACHE_CUSTOMERS,
                CACHE_LOAN_LIMITS

        );

        // Set TTL for customer cache, refresh cache after every 10 mins
        manager.registerCustomCache(CACHE_CUSTOMERS,
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(10_000)
                        .build()
        );

        // Set TTL for customerLoanLimits cache, refresh cache after every 5 mins
        manager.registerCustomCache(CACHE_LOAN_LIMITS,
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(10_000)
                        .build()
        );

        return manager;
    }
}
