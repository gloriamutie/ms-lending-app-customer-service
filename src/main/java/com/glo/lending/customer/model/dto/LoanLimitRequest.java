package com.glo.lending.customer.model.dto;

import com.glo.lending.customer.model.enums.RiskCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;
@Builder
@Data
public class LoanLimitRequest {
        @NotNull(message = "Customer ID is required")
        UUID customerId;

        @NotNull(message = "Max loan amount is required")
        @DecimalMin(value = "0.01", message = "Max loan amount must be greater than zero")
        BigDecimal maxLoanAmount;

        @Min(value = 0, message = "Credit score must be non-negative")
        Integer creditScore;

        @NotNull(message = "Risk category is required")
        RiskCategory riskCategory;
}
