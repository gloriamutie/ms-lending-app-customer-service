package com.glo.lending.customer.dblayer.repo;

import com.glo.lending.customer.dblayer.entities.LimitReservation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface LimitReservationRepository extends ReactiveCrudRepository<LimitReservation, UUID> {

    Mono<LimitReservation> findByIdempotencyKeyAndOperation(String idempotencyKey, String operation);
}

