package com.glo.lending.customer.service.serviceImpl;

import com.glo.lending.customer.components.CustomerCacheService;
import com.glo.lending.customer.exception.CustomerNotFoundException;
import com.glo.lending.customer.model.dto.*;
import com.glo.lending.customer.model.enums.CustomerStatus;
import com.glo.lending.customer.dblayer.entities.Customer;
import com.glo.lending.customer.dblayer.entities.CustomerFinancialHistory;
import com.glo.lending.customer.dblayer.entities.CustomerLoanLimit;
import com.glo.lending.customer.dblayer.repo.CustomerFinancialHistoryRepository;
import com.glo.lending.customer.dblayer.repo.CustomerLoanLimitRepository;
import com.glo.lending.customer.dblayer.repo.CustomerRepository;
import com.glo.lending.customer.service.CustomerService;
import com.glo.lending.customer.utils.CustomerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing customer profiles, loan limits, and financial history.
 */
@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final CustomerLoanLimitRepository loanLimitRepository;
    private final CustomerFinancialHistoryRepository financialHistoryRepository;
    private final CustomerCacheService cacheService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.loan-events}")
    private String customerEventsTopic;


    public CustomerServiceImpl(final CustomerRepository customerRepository,
                               final CustomerLoanLimitRepository loanLimitRepository,
                               final CustomerFinancialHistoryRepository financialHistoryRepository,
                               final CustomerCacheService cacheService,
                               final KafkaTemplate<String, Object> kafkaTemplate) {
        this.customerRepository = customerRepository;
        this.loanLimitRepository = loanLimitRepository;
        this.financialHistoryRepository = financialHistoryRepository;
        this.cacheService = cacheService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Mono<CustomerResponse> createCustomer(final CreateCustomerRequest request) {
        log.info("Creating customer: email={}", request.getEmail());
        final Customer customer = CustomerMapper.toEntity(request);
        return customerRepository.save(customer)
                .map(CustomerMapper::toResponse)
                .doOnSuccess(c -> log.info("Customer created: id={}", c.id()));
    }

    @Override
    public Mono<CustomerResponse> getCustomerById(final UUID customerId) {
        log.debug("Fetching customer: {}", customerId);
        return cacheService.getCustomerById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .map(CustomerMapper::toResponse);
    }

    @Override
    public Flux<CustomerResponse> getAllCustomers(final CustomerStatus status) {
        log.debug("Fetching customers, status={}", status);
        final Flux<Customer> customers = (status != null)
                ? customerRepository.findByStatus(status.name())
                : customerRepository.findAll();
        return customers.map(CustomerMapper::toResponse);
    }

    @Override
    public Mono<CustomerResponse> updateCustomer(final UUID customerId, final UpdateCustomerRequest request) {
        log.info("Updating customer: {}", customerId);
        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .flatMap(existing -> {
                    if (request.getFirstName() != null) existing.setFirstName(request.getFirstName());
                    if (request.getLastName() != null) existing.setLastName(request.getLastName());
                    if (request.getEmail() != null) existing.setEmail(request.getEmail());
                    if (request.getPhoneNumber() != null) existing.setPhoneNumber(request.getPhoneNumber());
                    if (request.getStatus() != null) existing.setStatus(request.getStatus());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return customerRepository.save(existing);
                })
                .doOnSuccess(c -> cacheService.evictCustomer(customerId))
                .map(CustomerMapper::toResponse);
    }

    @Override
    public Mono<LoanLimitResponse> setLoanLimit(final UUID customerId, final LoanLimitRequest request) {
        log.info("Setting loan limit for customer: {}", customerId);
        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .flatMap(customer -> loanLimitRepository.findByCustomerId(customerId)
                        .defaultIfEmpty(newLoanLimit(customerId))
                        .flatMap(limit -> {
                            limit.setMaxLoanAmount(request.getMaxLoanAmount());
                            limit.setAvailableAmount(request.getMaxLoanAmount());
                            limit.setCreditScore(request.getCreditScore());
                            limit.setRiskCategory(request.getRiskCategory());
                            limit.setLastAssessedAt(LocalDateTime.now());
                            return loanLimitRepository.save(limit);
                        })
                )
                .map(CustomerMapper::toLoanLimitResponse)
                .doOnSuccess(limitResponse -> {
                    cacheService.evictCustomer(customerId);
                    kafkaTemplate.send(customerEventsTopic, customerId.toString(),
                            Map.of("eventType", "LIMIT_UPDATED",
                                    "customerId", customerId,
                                    "maxLoanAmount", limitResponse.maxLoanAmount()))
                            .whenComplete((result, ex) -> {
                                if (ex != null) {
                                    log.error("Failed to publish LIMIT_UPDATED to Kafka: customerId={}", customerId, ex);
                                } else {
                                    log.info("Published LIMIT_UPDATED to Kafka: customerId={}, topic={}, partition={}, offset={}",
                                            customerId,
                                            result.getRecordMetadata().topic(),
                                            result.getRecordMetadata().partition(),
                                            result.getRecordMetadata().offset());
                                }
                            });
                });
    }

    @Override
    public Mono<LoanLimitResponse> getLoanLimit(final UUID customerId) {
        return cacheService.getLoanLimit(customerId)
                .switchIfEmpty(Mono.error(new IllegalStateException("No loan limit configured for customer: " + customerId)))
                .map(CustomerMapper::toLoanLimitResponse);
    }


    @Override
    public Flux<CustomerFinancialHistory> getFinancialHistory(final UUID customerId) {
        return financialHistoryRepository.findByCustomerIdOrderByRecordedAtDesc(customerId);
    }

    private CustomerLoanLimit newLoanLimit(final UUID customerId) {
        final CustomerLoanLimit limit = new CustomerLoanLimit();
        limit.setCustomerId(customerId);
        return limit;
    }
}

