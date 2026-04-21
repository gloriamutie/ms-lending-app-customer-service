package com.glo.lending.customer.dblayer.repo;

import com.glo.lending.customer.dblayer.entities.CustomerLoanLimit;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CustomerLoanLimitRepository extends ReactiveCrudRepository<CustomerLoanLimit, UUID> {

    Mono<CustomerLoanLimit> findByCustomerId(UUID customerId);
}

