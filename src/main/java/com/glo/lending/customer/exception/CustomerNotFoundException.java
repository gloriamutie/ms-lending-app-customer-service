package com.glo.lending.customer.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(final UUID customerId) {

        super("Customer not found with id: " + customerId);
    }
}

