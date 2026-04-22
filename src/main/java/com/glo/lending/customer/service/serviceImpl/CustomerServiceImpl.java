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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 *  service for managing customer profiles, loan limits, and financial history.
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);
    private static final String CUSTOMER_EVENTS_TOPIC = "lendingCustomerEventsv2";

    private final CustomerRepository customerRepository;
    private final CustomerLoanLimitRepository loanLimitRepository;
    private final CustomerFinancialHistoryRepository financialHistoryRepository;
    private final CustomerCacheService cacheService;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Override
    public Mono<CustomerResponse> createCustomer( CreateCustomerRequest request) {
        log.info("Creating customer: email={}", request.getEmail());
         Customer customer = CustomerMapper.toEntity(request);
        return customerRepository.save(customer)
                .map(CustomerMapper::toResponse)
                .doOnSuccess(c -> log.info("Customer created: id={}", c.id()));
    }


    @Override
    public Mono<CustomerResponse> getCustomerById( UUID customerId) {
        log.debug("Fetching customer: {}", customerId);
        return cacheService.getCustomerById(customerId).switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .map(CustomerMapper::toResponse);
    }


    @Override
    public Flux<CustomerResponse> getAllCustomers( CustomerStatus status) {
        log.debug("Fetching customers, status={}", status);
        Flux<Customer> customers = (status != null) ? customerRepository.findByStatus(status.name()) : customerRepository.findAll();
        return customers.map(CustomerMapper::toResponse);
    }

    @Override
    public Mono<CustomerResponse> updateCustomer( UUID customerId,  UpdateCustomerRequest request) {
        log.info("Updating customer: {}", customerId);
        return customerRepository.findById(customerId).switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
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
    public Mono<LoanLimitResponse> setLoanLimit( UUID customerId,  LoanLimitRequest request) {
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
                .doOnSuccess(saved -> {
                    cacheService.evictCustomer(customerId);
                    try {
                        kafkaTemplate.send(CUSTOMER_EVENTS_TOPIC, customerId.toString(),
                                Map.of("eventType", "LIMIT_UPDATED", "customerId", customerId,
                                        "maxLoanAmount", saved.getMaxLoanAmount()));
                        log.info("Published LIMIT_UPDATED event: customerId={}, max={}, topic={}", customerId, saved.getMaxLoanAmount(), CUSTOMER_EVENTS_TOPIC);
                    } catch (Exception e) {
                        log.error("Failed to publish Kafka event for customer: {}", customerId, e);
                    }
                })
                .map(CustomerMapper::toLoanLimitResponse);
    }


    @Override
    public Mono<LoanLimitResponse> getLoanLimit( UUID customerId) {
        return cacheService.getLoanLimit(customerId)
                .switchIfEmpty(Mono.error(new IllegalStateException("No loan limit configured for customer: " + customerId)))
                .map(CustomerMapper::toLoanLimitResponse);
    }

    @Override
    public Flux<CustomerFinancialHistory> getFinancialHistory( UUID customerId) {
        return financialHistoryRepository.findByCustomerIdOrderByRecordedAtDesc(customerId);
    }

    private CustomerLoanLimit newLoanLimit( UUID customerId) {
         CustomerLoanLimit limit = new CustomerLoanLimit();
        limit.setCustomerId(customerId);
        return limit;
    }
}

