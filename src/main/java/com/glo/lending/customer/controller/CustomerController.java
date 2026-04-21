package com.glo.lending.customer.controller;

import com.glo.lending.customer.model.dto.*;
import com.glo.lending.customer.model.enums.CustomerStatus;
import com.glo.lending.customer.dblayer.entities.CustomerFinancialHistory;
import com.glo.lending.customer.service.CustomerService;
import com.glo.lending.customer.service.LimitReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);
    private final CustomerService customerService;
    private final LimitReservationService limitReservationService;

    @PostMapping
    public Mono<ResponseEntity<CustomerResponse>> createCustomer(@Valid @RequestBody final CreateCustomerRequest request) {
        log.info("POST /api/v1/customers — email={}", request.getEmail());
        return customerService.createCustomer(request).map(r -> ResponseEntity.status(HttpStatus.CREATED).body(r));
    }

    @GetMapping("/{customerId}")
    public Mono<ResponseEntity<CustomerResponse>> getCustomer(@PathVariable final UUID customerId) {
        log.info("GET /api/v1/customers/{}", customerId);
        return customerService.getCustomerById(customerId).map(r -> ResponseEntity.ok().body(r));
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<CustomerResponse>>> getAllCustomers(@RequestParam(required = false) final CustomerStatus status) {
        log.info("GET /api/v1/customers — status={}", status);
        return Mono.just(ResponseEntity.ok(customerService.getAllCustomers(status)));
    }

    @PutMapping("/{customerId}")
    public Mono<ResponseEntity<CustomerResponse>> updateCustomer(@PathVariable final UUID customerId,
                                                                  @Valid @RequestBody final UpdateCustomerRequest request) {
        log.info("PUT /api/v1/customers/{}", customerId);
        return customerService.updateCustomer(customerId, request).map(r -> ResponseEntity.ok().body(r));
    }

    @PostMapping("/{customerId}/loan-limits")
    public Mono<ResponseEntity<LoanLimitResponse>> setLoanLimit(@PathVariable final UUID customerId,
                                                                 @Valid @RequestBody final LoanLimitRequest request) {
        log.info("POST /api/v1/customers/{}/loan-limits", customerId);
        return customerService.setLoanLimit(customerId, request).map(r -> ResponseEntity.ok().body(r));
    }

    @GetMapping("/{customerId}/loan-limits")
    public Mono<ResponseEntity<LoanLimitResponse>> getLoanLimit(@PathVariable final UUID customerId) {
        log.info("GET /api/v1/customers/{}/loan-limits", customerId);
        return customerService.getLoanLimit(customerId).map(r -> ResponseEntity.ok().body(r));
    }

    @GetMapping("/{customerId}/financial-history")
    public Mono<ResponseEntity<Flux<CustomerFinancialHistory>>> getFinancialHistory(@PathVariable final UUID customerId) {
        log.info("GET /api/v1/customers/{}/financial-history", customerId);
        return Mono.just(ResponseEntity.ok(customerService.getFinancialHistory(customerId)));
    }

    /** Called by Loan Service saga to reserve customer limit. */
    @PutMapping("/{customerId}/loan-limits/reserve")
    public Mono<ResponseEntity<Void>> reserveLimit(@PathVariable final UUID customerId,
                                                    @Valid @RequestBody final LimitReservationRequest request) {
        log.info("PUT /api/v1/customers/{}/loan-limits/reserve", customerId);
        return limitReservationService.reserveLimit(customerId, request).then(Mono.just(ResponseEntity.ok().build()));
    }

    /** Called by Loan Service saga compensation to release customer limit. */
    @PutMapping("/{customerId}/loan-limits/release")
    public Mono<ResponseEntity<Void>> releaseLimit(@PathVariable final UUID customerId,
                                                    @Valid @RequestBody final LimitReservationRequest request) {
        log.info("PUT /api/v1/customers/{}/loan-limits/release", customerId);
        return limitReservationService.releaseLimit(customerId, request).then(Mono.just(ResponseEntity.ok().build()));
    }
}

