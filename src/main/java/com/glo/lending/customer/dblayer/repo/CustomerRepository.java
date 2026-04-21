package com.glo.lending.customer.dblayer.repo;

import com.glo.lending.customer.dblayer.entities.Customer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;


@Repository
public interface CustomerRepository extends ReactiveCrudRepository<Customer, UUID> {

    Mono<Customer> findByEmail(String email);

    Mono<Customer> findByIdNumber(String idNumber);

    Flux<Customer> findByStatus(String status);
}

