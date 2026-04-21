package com.glo.lending.customer.service;

import com.glo.lending.customer.components.CustomerCacheService;
import com.glo.lending.customer.model.dto.LimitReservationRequest;
import com.glo.lending.customer.dblayer.entities.CustomerLoanLimit;
import com.glo.lending.customer.dblayer.entities.LimitReservation;
import com.glo.lending.customer.dblayer.repo.CustomerLoanLimitRepository;
import com.glo.lending.customer.dblayer.repo.LimitReservationRepository;
import com.glo.lending.customer.service.serviceImpl.LimitReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LimitReservationService Unit Tests")
class LimitReservationServiceTest {

    @Mock private CustomerLoanLimitRepository loanLimitRepository;
    @Mock private LimitReservationRepository reservationRepository;
    @Mock private CustomerCacheService cacheService;

    @InjectMocks private LimitReservationServiceImpl limitReservationService;

    private UUID customerId;
    private LimitReservationRequest request;
    private CustomerLoanLimit limit;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        request = new LimitReservationRequest(BigDecimal.valueOf(5000), UUID.randomUUID(), "IDEM-KEY-001");
        limit = new CustomerLoanLimit();
        limit.setId(UUID.randomUUID());
        limit.setCustomerId(customerId);
        limit.setMaxLoanAmount(BigDecimal.valueOf(100000));
        limit.setAvailableAmount(BigDecimal.valueOf(75000));
    }

    @Nested
    @DisplayName("reserveLimit")
    class ReserveLimit {

        @Test
        @DisplayName("should reserve limit when sufficient balance and not idempotent duplicate")
        void reserveLimit_SufficientBalance_Succeeds() {
            // Given
            when(reservationRepository.findByIdempotencyKeyAndOperation("IDEM-KEY-001", "RESERVE")).thenReturn(Mono.empty());
            when(loanLimitRepository.findByCustomerId(customerId)).thenReturn(Mono.just(limit));
            when(loanLimitRepository.save(any(CustomerLoanLimit.class))).thenReturn(Mono.just(limit));
            when(reservationRepository.save(any(LimitReservation.class))).thenReturn(Mono.just(new LimitReservation()));
            doNothing().when(cacheService).evictCustomer(customerId);

            // When & Then
            StepVerifier.create(limitReservationService.reserveLimit(customerId, request))
                    .verifyComplete();

            verify(loanLimitRepository).save(any());
        }

        @Test
        @DisplayName("should skip when idempotent key already processed")
        void reserveLimit_AlreadyProcessed_Skips() {
            // Given
            when(reservationRepository.findByIdempotencyKeyAndOperation("IDEM-KEY-001", "RESERVE"))
                    .thenReturn(Mono.just(new LimitReservation()));

            // When & Then
            StepVerifier.create(limitReservationService.reserveLimit(customerId, request))
                    .verifyComplete();

            verify(loanLimitRepository, never()).save(any());
        }

        @Test
        @DisplayName("should fail when insufficient balance")
        void reserveLimit_InsufficientBalance_ThrowsException() {
            // Given
            limit.setAvailableAmount(BigDecimal.valueOf(1000)); // Less than 5000 requested
            when(reservationRepository.findByIdempotencyKeyAndOperation("IDEM-KEY-001", "RESERVE")).thenReturn(Mono.empty());
            when(loanLimitRepository.findByCustomerId(customerId)).thenReturn(Mono.just(limit));

            // When & Then
            StepVerifier.create(limitReservationService.reserveLimit(customerId, request))
                    .expectErrorMatches(e -> e instanceof IllegalStateException && e.getMessage().contains("Insufficient"))
                    .verify();
        }

        @Test
        @DisplayName("should fail when no limit configured")
        void reserveLimit_NoLimit_ThrowsException() {
            // Given
            when(reservationRepository.findByIdempotencyKeyAndOperation("IDEM-KEY-001", "RESERVE")).thenReturn(Mono.empty());
            when(loanLimitRepository.findByCustomerId(customerId)).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(limitReservationService.reserveLimit(customerId, request))
                    .expectErrorMatches(e -> e instanceof IllegalStateException && e.getMessage().contains("No loan limit"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("releaseLimit")
    class ReleaseLimit {

        @Test
        @DisplayName("should release limit successfully")
        void releaseLimit_ValidRequest_Succeeds() {
            // Given
            when(reservationRepository.findByIdempotencyKeyAndOperation("IDEM-KEY-001", "RELEASE")).thenReturn(Mono.empty());
            when(loanLimitRepository.findByCustomerId(customerId)).thenReturn(Mono.just(limit));
            when(loanLimitRepository.save(any(CustomerLoanLimit.class))).thenReturn(Mono.just(limit));
            when(reservationRepository.save(any(LimitReservation.class))).thenReturn(Mono.just(new LimitReservation()));
            doNothing().when(cacheService).evictCustomer(customerId);

            // When & Then
            StepVerifier.create(limitReservationService.releaseLimit(customerId, request))
                    .verifyComplete();

            verify(loanLimitRepository).save(any());
        }

        @Test
        @DisplayName("should skip when release already processed")
        void releaseLimit_AlreadyProcessed_Skips() {
            // Given
            when(reservationRepository.findByIdempotencyKeyAndOperation("IDEM-KEY-001", "RELEASE"))
                    .thenReturn(Mono.just(new LimitReservation()));

            // When & Then
            StepVerifier.create(limitReservationService.releaseLimit(customerId, request))
                    .verifyComplete();

            verify(loanLimitRepository, never()).save(any());
        }
    }
}

