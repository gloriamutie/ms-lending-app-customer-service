package com.glo.lending.customer.service;

import com.glo.lending.customer.model.dto.LimitReservationRequest;
import com.glo.lending.customer.dblayer.entities.LimitReservation;
import com.glo.lending.customer.dblayer.repo.CustomerLoanLimitRepository;
import com.glo.lending.customer.dblayer.repo.LimitReservationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class LimitReservationService {

    private static final Logger log = LoggerFactory.getLogger(LimitReservationService.class);
    private static final String OPERATION_RESERVE = "RESERVE";
    private static final String OPERATION_RELEASE = "RELEASE";

    private final CustomerLoanLimitRepository loanLimitRepository;
    private final LimitReservationRepository reservationRepository;
    private final CustomerCacheService cacheService;


    /**
     * Atomically reserves (decrements) a portion of the customer's available limit.
     * <p>
     * Idempotent: if a reservation with the same idempotency key already exists,
     * the operation succeeds without modifying the limit again.
     * </p>
     *
     * @param customerId the customer whose limit to reserve
     * @param request    the reservation details including amount and idempotency key
     * @return a {@link Mono} that completes when the reservation is persisted
     * @throws IllegalStateException if the customer has insufficient available limit
     */
    public Mono<Void> reserveLimit(final UUID customerId, final LimitReservationRequest request) {
        return reservationRepository.findByIdempotencyKeyAndOperation(request.getIdempotencyKey(), OPERATION_RESERVE)
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        log.info("Idempotent reserve: already processed for key={}", request.getIdempotencyKey());
                        return Mono.empty();
                    }
                    return executeReservation(customerId, request);
                });
    }

    /**
     * Atomically releases (increments) a portion of the customer's available limit.
     * <p>
     * Used as compensation during saga rollback. Idempotent via idempotency key.
     * </p>
     *
     * @param customerId the customer whose limit to release
     * @param request    the release details including amount and idempotency key
     * @return a {@link Mono} that completes when the release is persisted
     */
    public Mono<Void> releaseLimit(final UUID customerId, final LimitReservationRequest request) {
        return reservationRepository.findByIdempotencyKeyAndOperation(request.getIdempotencyKey(), OPERATION_RELEASE)
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        log.info("Idempotent release: already processed for key={}", request.getIdempotencyKey());
                        return Mono.empty();
                    }
                    return executeRelease(customerId, request);
                });
    }

    private Mono<Void> executeReservation(final UUID customerId, final LimitReservationRequest request) {
        return loanLimitRepository.findByCustomerId(customerId)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "No loan limit configured for customer: " + customerId)))
                .flatMap(limit -> {
                    if (limit.getAvailableAmount().compareTo(request.getAmount()) < 0) {
                        return Mono.error(new IllegalStateException(
                                String.format("Insufficient limit. Available: %s, Requested: %s",
                                        limit.getAvailableAmount(), request.getAmount())));
                    }

                    limit.setAvailableAmount(limit.getAvailableAmount().subtract(request.getAmount()));
                    return loanLimitRepository.save(limit)
                            .flatMap(savedLimit -> saveReservationRecord(customerId, request, OPERATION_RESERVE));
                })
                .doOnSuccess(v -> {
                    cacheService.evictCustomer(customerId);
                    log.info("Limit reserved: customerId={}, amount={}, key={}",
                            customerId, request.getAmount(), request.getIdempotencyKey());
                });
    }

    private Mono<Void> executeRelease(final UUID customerId, final LimitReservationRequest request) {
        return loanLimitRepository.findByCustomerId(customerId)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "No loan limit configured for customer: " + customerId)))
                .flatMap(limit -> {
                    final BigDecimal newAvailable = limit.getAvailableAmount().add(request.getAmount());
                    // Cap at max loan amount
                    limit.setAvailableAmount(
                            newAvailable.min(limit.getMaxLoanAmount()));
                    return loanLimitRepository.save(limit)
                            .flatMap(savedLimit -> saveReservationRecord(customerId, request, OPERATION_RELEASE));
                })
                .doOnSuccess(v -> {
                    cacheService.evictCustomer(customerId);
                    log.info("Limit released: customerId={}, amount={}, key={}",
                            customerId, request.getAmount(), request.getIdempotencyKey());
                });
    }

    private Mono<Void> saveReservationRecord(final UUID customerId,
                                              final LimitReservationRequest request,
                                              final String operation) {
        final LimitReservation reservation = new LimitReservation();
        reservation.setCustomerId(customerId);
        reservation.setLoanId(request.getLoanId());
        reservation.setIdempotencyKey(request.getIdempotencyKey());
        reservation.setAmount(request.getAmount());
        reservation.setOperation(operation);
        reservation.setCreatedAt(LocalDateTime.now());
        return reservationRepository.save(reservation).then();
    }
}

