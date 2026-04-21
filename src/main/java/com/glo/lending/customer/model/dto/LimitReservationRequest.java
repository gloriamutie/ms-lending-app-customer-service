package com.glo.lending.customer.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LimitReservationRequest {
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount;
        @NotNull(message = "Loan ID is required")
        UUID loanId;
        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey;
}

