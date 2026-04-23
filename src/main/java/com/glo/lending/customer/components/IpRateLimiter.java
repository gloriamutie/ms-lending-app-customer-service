package com.glo.lending.customer.components;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IpRateLimiter {

    private static class RequestBucket {
        int count;
        Instant windowStart;
    }

    private final Map<String, RequestBucket> cache = new ConcurrentHashMap<>();

    private static final int LIMIT = 5; // requests
    private static final Duration WINDOW = Duration.ofSeconds(10);

    public void validate(String ip) {
        Instant now = Instant.now();
        RequestBucket bucket = cache.computeIfAbsent(ip, k -> {
            RequestBucket b = new RequestBucket();
            b.count = 0;
            b.windowStart = now;
            return b;
        });

        synchronized (bucket) {
            // reset window
            if (now.isAfter(bucket.windowStart.plus(WINDOW))) {
                bucket.count = 0;
                bucket.windowStart = now;
            }

            bucket.count++;

            if (bucket.count > LIMIT) {throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many requests from IP: " + ip
                );
            }
        }
    }
}
