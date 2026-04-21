package com.glo.lending.customer.service;

import com.glo.lending.customer.dblayer.entities.CustomerFinancialHistory;
import com.glo.lending.customer.model.dto.*;
import com.glo.lending.customer.model.enums.CustomerStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CustomerService {
    Mono<CustomerResponse> createCustomer( CreateCustomerRequest request);
    Mono<CustomerResponse> getCustomerById( java.util.UUID customerId);
    Flux<CustomerResponse> getAllCustomers( CustomerStatus status);
    Mono<CustomerResponse> updateCustomer( UUID customerId,  UpdateCustomerRequest request);
    Mono<LoanLimitResponse> setLoanLimit(UUID customerId, LoanLimitRequest request);
    Mono<LoanLimitResponse> getLoanLimit(final UUID customerId);
    Flux<CustomerFinancialHistory> getFinancialHistory(final UUID customerId);
}
