package com.glo.lending.customer.service.serviceImpl;

import com.glo.lending.customer.components.CustomerCacheService;
import com.glo.lending.customer.model.dto.LimitReservationRequest;
import com.glo.lending.customer.dblayer.entities.LimitReservation;
import com.glo.lending.customer.dblayer.repo.CustomerLoanLimitRepository;
import com.glo.lending.customer.dblayer.repo.LimitReservationRepository;
import com.glo.lending.customer.service.LimitReservationService;
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
public class LimitReservationServiceImpl implements LimitReservationService {

    private static final Logger log = LoggerFactory.getLogger(LimitReservationServiceImpl.class);
    private static final String OPERATION_RESERVE = "RESERVE";
    private static final String OPERATION_RELEASE = "RELEASE";

    private final CustomerLoanLimitRepository loanLimitRepository;
    private final LimitReservationRepository reservationRepository;
    private final CustomerCacheService cacheService;



    @Override
    public Mono<Void> reserveLimit( UUID customerId,  LimitReservationRequest request) {
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

    @Override
    public Mono<Void> releaseLimit( UUID customerId,  LimitReservationRequest request) {
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

    private Mono<Void> executeReservation( UUID customerId,  LimitReservationRequest request) {
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

    private Mono<Void> executeRelease( UUID customerId,  LimitReservationRequest request) {
        return loanLimitRepository.findByCustomerId(customerId)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "No loan limit configured for customer: " + customerId)))
                .flatMap(limit -> {
                     BigDecimal newAvailable = limit.getAvailableAmount().add(request.getAmount());
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

    private Mono<Void> saveReservationRecord( UUID customerId,  LimitReservationRequest request, String operation) {
        LimitReservation reservation = LimitReservation.builder()
                .customerId(customerId)
                .loanId(request.getLoanId())
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .operation(operation)
                .createdAt(LocalDateTime.now())
                .build();

        return reservationRepository.save(reservation).then();
    }
}

