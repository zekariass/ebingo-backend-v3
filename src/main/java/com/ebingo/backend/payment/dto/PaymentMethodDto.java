package com.ebingo.backend.payment.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentMethodDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isDefault;

    private Boolean isOnline; // new field
    private Boolean isMobileMoney;
    private String instructionUrl; // new field
    private Boolean withdrawalRequirePassword;
    private String logoUrl;
    private Instant createdAt;
    private Instant updatedAt;

}

