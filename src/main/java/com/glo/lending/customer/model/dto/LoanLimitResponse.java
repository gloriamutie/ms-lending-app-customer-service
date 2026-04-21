package com.glo.lending.customer.model.dto;

import com.glo.lending.customer.model.enums.RiskCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a customer's loan limit.
 *
 * @param id              unique limit record identifier
 * @param customerId      associated customer
 * @param maxLoanAmount   maximum borrowing limit
 * @param availableAmount currently available borrowing capacity
 * @param creditScore     credit score
 * @param riskCategory    risk classification
 * @param lastAssessedAt  last assessment timestamp
 */
public record LoanLimitResponse(
        UUID id,
        UUID customerId,
        BigDecimal maxLoanAmount,
        BigDecimal availableAmount,
        Integer creditScore,
        RiskCategory riskCategory,
        LocalDateTime lastAssessedAt
) {
}

