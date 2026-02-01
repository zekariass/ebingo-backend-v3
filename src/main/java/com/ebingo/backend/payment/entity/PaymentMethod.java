package com.ebingo.backend.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("payment_method")
public class PaymentMethod {

    @Id
    private Long id;

    private String code;

    private String name;

    private String description;

    private Boolean isDefault;

    private Boolean isOnline; // new field

    private Boolean isMobileMoney; // new field

    private String instructionUrl; // new field

    private Boolean withdrawalRequirePassword;

    private String logoUrl;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}
