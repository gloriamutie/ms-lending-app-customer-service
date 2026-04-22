package com.glo.lending.customer.components;

import com.glo.lending.customer.config.CacheConfig;
import com.glo.lending.customer.dblayer.entities.Customer;
import com.glo.lending.customer.dblayer.entities.CustomerLoanLimit;
import com.glo.lending.customer.dblayer.repo.CustomerLoanLimitRepository;
import com.glo.lending.customer.dblayer.repo.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;


@RequiredArgsConstructor
@Service
public class CustomerCacheService {

    private static final Logger log = LoggerFactory.getLogger(CustomerCacheService.class);

    private final CustomerRepository customerRepository;
    private final CustomerLoanLimitRepository loanLimitRepository;
    private final CacheManager cacheManager;


    public Mono<Customer> getCustomerById(final UUID customerId) {
         Cache cache = cacheManager.getCache(CacheConfig.CACHE_CUSTOMERS);
        if (cache != null) {
             Customer cached = cache.get(customerId, Customer.class);
            if (cached != null) {
                log.debug("Cache HIT for customer: {}", customerId);
                return Mono.just(cached);
            }
        }
        log.debug("Cache MISS for customer: {}", customerId);
        return customerRepository.findById(customerId)
                .doOnNext(customer -> {
                    if (cache != null) {
                        cache.put(customerId, customer);
                    }
                });
    }

    public Mono<CustomerLoanLimit> getLoanLimit( UUID customerId) {
        final Cache cache = cacheManager.getCache(CacheConfig.CACHE_LOAN_LIMITS);
        if (cache != null) {
            final CustomerLoanLimit cached = cache.get(customerId, CustomerLoanLimit.class);
            if (cached != null) {
                log.debug("Cache HIT for loan limit: {}", customerId);
                return Mono.just(cached);
            }
        }
        log.debug("Cache MISS for loan limit: {}", customerId);
        return loanLimitRepository.findByCustomerId(customerId)
                .doOnNext(limit -> {
                    if (cache != null) {
                        cache.put(customerId, limit);
                    }
                });
    }

    public void evictCustomer(final UUID customerId) {
        log.info("Evicting cache entries for customer: {}", customerId);
        evictFromCache(CacheConfig.CACHE_CUSTOMERS, customerId);
        evictFromCache(CacheConfig.CACHE_LOAN_LIMITS, customerId);
    }

    private void evictFromCache(final String cacheName, final UUID key) {
        final Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
