package com.glo.lending.customer.service;

import com.glo.lending.customer.components.CustomerCacheService;
import com.glo.lending.customer.exception.CustomerNotFoundException;
import com.glo.lending.customer.model.dto.*;
import com.glo.lending.customer.model.enums.CustomerStatus;
import com.glo.lending.customer.model.enums.RiskCategory;
import com.glo.lending.customer.dblayer.entities.Customer;
import com.glo.lending.customer.dblayer.entities.CustomerFinancialHistory;
import com.glo.lending.customer.dblayer.entities.CustomerLoanLimit;
import com.glo.lending.customer.dblayer.repo.CustomerFinancialHistoryRepository;
import com.glo.lending.customer.dblayer.repo.CustomerLoanLimitRepository;
import com.glo.lending.customer.dblayer.repo.CustomerRepository;
import com.glo.lending.customer.service.serviceImpl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerLoanLimitRepository loanLimitRepository;
    @Mock private CustomerFinancialHistoryRepository financialHistoryRepository;
    @Mock private CustomerCacheService cacheService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks private CustomerServiceImpl customerService;

    private Customer customer;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customer = new Customer();
        customer.setId(customerId);
        customer.setFirstName("john");
        customer.setLastName("doe");
        customer.setEmail("johndoe@email.com");
        customer.setPhoneNumber("+2547***0966");
        customer.setIdNumber("ID12345678");
        customer.setDateOfBirth(LocalDate.of(2000, 5, 15));
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createCustomer")
    class CreateCustomer {

        @Test
        @DisplayName("should create customer successfully")
        void createCustomer_ValidRequest_ReturnsCustomerResponse() {
            // Given
           CreateCustomerRequest request =  CreateCustomerRequest.builder()
                   .firstName("john")
                   .lastName("doe")
                   .email("johndoe@email.com")
                   .phoneNumber("+254****5678")
                   .idNumber("ID12345678")
                   .dateOfBirth(LocalDate.of(1996, 5, 15))
                   .status(CustomerStatus.ACTIVE)
                   .build();

            when(customerRepository.save(any(Customer.class))).thenReturn(Mono.just(customer));

            // When & Then
            StepVerifier.create(customerService.createCustomer(request))
                    .assertNext(response -> {
                        assertEquals("john", response.firstName());
                        assertEquals("johndoe@email.com", response.email());
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getCustomerById")
    class GetCustomerById {

        @Test
        @DisplayName("should return customer from cache")
        void getCustomerById_Exists_ReturnsCustomer() {
            // Given
            when(cacheService.getCustomerById(customerId)).thenReturn(Mono.just(customer));

            // When & Then
            StepVerifier.create(customerService.getCustomerById(customerId))
                    .assertNext(r -> assertEquals(customerId, r.id()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw when customer not found")
        void getCustomerById_NotExists_ThrowsException() {
            // Given
            when(cacheService.getCustomerById(customerId)).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(customerService.getCustomerById(customerId))
                    .expectError(CustomerNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("getAllCustomers")
    class GetAllCustomers {

        @Test
        @DisplayName("should return all customers when no status filter")
        void getAllCustomers_NoFilter_ReturnsAll() {
            // Given
            when(customerRepository.findAll()).thenReturn(Flux.just(customer));

            // When & Then
            StepVerifier.create(customerService.getAllCustomers(null))
                    .assertNext(r -> assertEquals("john", r.firstName()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should filter by status")
        void getAllCustomers_WithFilter_ReturnsFiltered() {
            // Given
            when(customerRepository.findByStatus("ACTIVE")).thenReturn(Flux.just(customer));

            // When & Then
            StepVerifier.create(customerService.getAllCustomers(CustomerStatus.ACTIVE))
                    .expectNextCount(1)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("updateCustomer")
    class UpdateCustomer {

        @Test
        @DisplayName("should update non-null fields only")
        void updateCustomer_PartialUpdate_UpdatesOnlyProvided() {
            // Given
            UpdateCustomerRequest request = UpdateCustomerRequest.builder()
                    .firstName("Updated")
                    .lastName(null)
                    .email(null)
                    .phoneNumber(null)
                    .status(null)
                    .build();
            when(customerRepository.findById(customerId)).thenReturn(Mono.just(customer));
            when(customerRepository.save(any(Customer.class))).thenReturn(Mono.just(customer));

            // When & Then
            StepVerifier.create(customerService.updateCustomer(customerId, request))
                    .assertNext(r -> assertNotNull(r.id()))
                    .verifyComplete();

            verify(cacheService).evictCustomer(customerId);
        }

        @Test
        @DisplayName("should throw when customer not found")
        void updateCustomer_NotFound_ThrowsException() {
            // Given
            UpdateCustomerRequest request = UpdateCustomerRequest.builder()
                    .firstName(null)
                    .lastName(null)
                    .email(null)
                    .phoneNumber(null)
                    .status(null)
                    .build();
            when(customerRepository.findById(customerId)).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(customerService.updateCustomer(customerId, request))
                    .expectError(CustomerNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("setLoanLimit")
    class SetLoanLimit {

        @Test
        @DisplayName("should set loan limit for existing customer")
        void setLoanLimit_ValidRequest_ReturnsLoanLimitResponse() {
            // Given
            LoanLimitRequest request = LoanLimitRequest.builder()
                    .customerId(customerId)
                    .maxLoanAmount(BigDecimal.valueOf(100000))
                    .creditScore(720)
                    .riskCategory(RiskCategory.LOW)
                    .build();


            final CustomerLoanLimit limit = new CustomerLoanLimit();
            limit.setId(UUID.randomUUID());
            limit.setCustomerId(customerId);
            limit.setMaxLoanAmount(BigDecimal.valueOf(100000));
            limit.setAvailableAmount(BigDecimal.valueOf(100000));
            limit.setCreditScore(720);
            limit.setRiskCategory(RiskCategory.LOW);
            limit.setLastAssessedAt(LocalDateTime.now());

            when(customerRepository.findById(customerId)).thenReturn(Mono.just(customer));
            when(loanLimitRepository.findByCustomerId(customerId)).thenReturn(Mono.empty());
            when(loanLimitRepository.save(any(CustomerLoanLimit.class))).thenReturn(Mono.just(limit));
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

            // When & Then
            StepVerifier.create(customerService.setLoanLimit(customerId, request))
                    .assertNext(r -> {
                        assertEquals(BigDecimal.valueOf(100000), r.maxLoanAmount());
                        assertEquals(RiskCategory.LOW, r.riskCategory());
                    })
                    .verifyComplete();

            verify(cacheService).evictCustomer(customerId);
        }

        @Test
        @DisplayName("should throw when customer not found")
        void setLoanLimit_CustomerNotFound_ThrowsException() {
            // Given
            LoanLimitRequest request = LoanLimitRequest.builder()
                    .customerId(customerId)
                    .maxLoanAmount(BigDecimal.TEN)
                    .creditScore(500)
                    .riskCategory(RiskCategory.MEDIUM)
                    .build();

            when(customerRepository.findById(customerId)).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(customerService.setLoanLimit(customerId, request))
                    .expectError(CustomerNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("getLoanLimit")
    class GetLoanLimit {

        @Test
        @DisplayName("should return loan limit from cache")
        void getLoanLimit_Exists_ReturnsLimit() {
            // Given
            final CustomerLoanLimit limit = new CustomerLoanLimit();
            limit.setId(UUID.randomUUID());
            limit.setCustomerId(customerId);
            limit.setMaxLoanAmount(BigDecimal.valueOf(50000));
            limit.setAvailableAmount(BigDecimal.valueOf(50000));
            limit.setRiskCategory(RiskCategory.MEDIUM);
            limit.setLastAssessedAt(LocalDateTime.now());
            when(cacheService.getLoanLimit(customerId)).thenReturn(Mono.just(limit));

            // When & Then
            StepVerifier.create(customerService.getLoanLimit(customerId))
                    .assertNext(r -> assertEquals(BigDecimal.valueOf(50000), r.maxLoanAmount()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw when no limit configured")
        void getLoanLimit_NotExists_ThrowsException() {
            // Given
            when(cacheService.getLoanLimit(customerId)).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(customerService.getLoanLimit(customerId))
                    .expectError(IllegalStateException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("getFinancialHistory")
    class GetFinancialHistory {

        @Test
        @DisplayName("should return financial history for customer")
        void getFinancialHistory_HasRecords_ReturnsFlux() {
            // Given
            final CustomerFinancialHistory record = new CustomerFinancialHistory();
            record.setId(UUID.randomUUID());
            record.setCustomerId(customerId);
            when(financialHistoryRepository.findByCustomerIdOrderByRecordedAtDesc(customerId))
                    .thenReturn(Flux.just(record));

            // When & Then
            StepVerifier.create(customerService.getFinancialHistory(customerId))
                    .assertNext(r -> assertEquals(customerId, r.getCustomerId()))
                    .verifyComplete();
        }
    }
}

