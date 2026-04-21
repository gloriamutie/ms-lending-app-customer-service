package com.glo.lending.customer.service;

import com.glo.lending.customer.model.dto.LimitReservationRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface LimitReservationService {
    Mono<Void> reserveLimit(final UUID customerId, final LimitReservationRequest request);
    Mono<Void> releaseLimit(final UUID customerId, final LimitReservationRequest request);

}
