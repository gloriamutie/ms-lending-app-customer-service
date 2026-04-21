package com.glo.lending.customer.dblayer.repo;

import com.glo.lending.customer.dblayer.entities.CustomerFinancialHistory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;


@Repository
public interface CustomerFinancialHistoryRepository extends ReactiveCrudRepository<CustomerFinancialHistory, UUID> {

    Flux<CustomerFinancialHistory> findByCustomerIdOrderByRecordedAtDesc(UUID customerId);
}

