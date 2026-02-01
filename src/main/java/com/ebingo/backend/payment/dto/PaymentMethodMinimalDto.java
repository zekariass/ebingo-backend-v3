package com.ebingo.backend.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentMethodMinimalDto {
    private Long id;
    private String code;
    private String name;
    private String description;
}

