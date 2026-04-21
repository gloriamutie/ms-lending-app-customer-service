package com.glo.lending.customer.dblayer.entities;

import com.glo.lending.customer.model.enums.RiskCategory;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("customer_loan_limits")
public class CustomerLoanLimit {

    @Id
    private UUID id;
    @Column("customer_id")
    private UUID customerId;
    @Column("max_loan_amount")
    private BigDecimal maxLoanAmount;
    @Column("available_amount")
    private BigDecimal availableAmount;
    @Column("credit_score")
    private Integer creditScore;
    @Column("risk_category")
    private RiskCategory riskCategory;
    @LastModifiedDate
    @Column("last_assessed_at")
    private LocalDateTime lastAssessedAt;
}

