package com.glo.lending.customer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String CACHE_CUSTOMERS = "customers";
    public static final String CACHE_LOAN_LIMITS = "customerLoanLimits";

    @Bean
    public CacheManager cacheManager() {
        log.info("Initializing Spring ConcurrentMapCacheManager with caches: {}, {}",
                CACHE_CUSTOMERS, CACHE_LOAN_LIMITS);
        return new ConcurrentMapCacheManager(CACHE_CUSTOMERS, CACHE_LOAN_LIMITS);
    }
}
