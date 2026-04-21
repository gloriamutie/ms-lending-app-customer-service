package com.glo.lending.customer.dblayer.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Table("customer_financial_history")
public class CustomerFinancialHistory {

    @Id
    private UUID id;
    @Column("customer_id")
    private UUID customerId;
    @Column("record_type")
    private String recordType;
    @Column("description")
    private String description;
    @Column("amount")
    private BigDecimal amount;
    @Column("recorded_at")
    private LocalDateTime recordedAt;

}

