package com.glo.lending.customer.utils;

import com.glo.lending.customer.model.dto.CreateCustomerRequest;
import com.glo.lending.customer.model.dto.CustomerResponse;
import com.glo.lending.customer.model.dto.LoanLimitResponse;
import com.glo.lending.customer.dblayer.entities.Customer;
import com.glo.lending.customer.dblayer.entities.CustomerLoanLimit;

import java.time.LocalDateTime;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static Customer toEntity(final CreateCustomerRequest req) {
        return Customer.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phoneNumber(req.getPhoneNumber())
                .idNumber(req.getIdNumber())
                .dateOfBirth(req.getDateOfBirth())
                .status(req.getStatus())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

    }

    public static CustomerResponse toResponse(final Customer c) {
        return new CustomerResponse(c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(),
                c.getPhoneNumber(), c.getIdNumber(), c.getDateOfBirth(), c.getStatus(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    public static LoanLimitResponse toLoanLimitResponse(final CustomerLoanLimit l) {
        return new LoanLimitResponse(l.getId(), l.getCustomerId(), l.getMaxLoanAmount(),
                l.getAvailableAmount(), l.getCreditScore(), l.getRiskCategory(), l.getLastAssessedAt());
    }
}

