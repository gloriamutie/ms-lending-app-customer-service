package com.glo.lending.customer.model.dto;

import com.glo.lending.customer.model.enums.CustomerStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a customer profile.
 *
 * @param id          unique customer identifier
 * @param firstName   customer's first name
 * @param lastName    customer's last name
 * @param email       email address
 * @param phoneNumber phone number
 * @param idNumber    national ID / passport
 * @param dateOfBirth date of birth
 * @param status      current customer status
 * @param createdAt   account creation timestamp
 * @param updatedAt   last update timestamp
 */
public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String idNumber,
        LocalDate dateOfBirth,
        CustomerStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

