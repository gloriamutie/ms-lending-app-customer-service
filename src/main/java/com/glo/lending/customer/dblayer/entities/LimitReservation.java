package com.glo.lending.customer.dblayer.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("limit_reservations")
public class LimitReservation {

    @Id
    private UUID id;
    @Column("customer_id")
    private UUID customerId;
    @Column("loan_id")
    private UUID loanId;
    @Column("idempotency_key")
    private String idempotencyKey;
    @Column("amount")
    private BigDecimal amount;
    /** RESERVE or RELEASE */
    @Column("operation")
    private String operation;
    @Column("created_at")
    private LocalDateTime createdAt;

}

