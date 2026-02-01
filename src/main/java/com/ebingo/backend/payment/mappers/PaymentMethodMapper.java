package com.ebingo.backend.payment.mappers;

import com.ebingo.backend.payment.dto.PaymentMethodDto;
import com.ebingo.backend.payment.dto.PaymentMethodMinimalDto;
import com.ebingo.backend.payment.entity.PaymentMethod;

public final class PaymentMethodMapper {
    public static PaymentMethodDto toDto(PaymentMethod paymentMethod) {
        return PaymentMethodDto.builder()
                .id(paymentMethod.getId())
                .code(paymentMethod.getCode())
                .name(paymentMethod.getName())
                .description(paymentMethod.getDescription())
                .isDefault(paymentMethod.getIsDefault())
                .isOnline(paymentMethod.getIsOnline())
                .isMobileMoney(paymentMethod.getIsMobileMoney())
                .instructionUrl(paymentMethod.getInstructionUrl())
                .logoUrl(paymentMethod.getLogoUrl())
                .withdrawalRequirePassword(paymentMethod.getWithdrawalRequirePassword())
                .createdAt(paymentMethod.getCreatedAt())
                .updatedAt(paymentMethod.getUpdatedAt())
                .build();
    }

    public static PaymentMethod toEntity(PaymentMethodDto paymentMethod) {
        PaymentMethod p = new PaymentMethod();
        p.setId(paymentMethod.getId());
        p.setCode(paymentMethod.getCode());
        p.setName(paymentMethod.getName());
        p.setDescription(paymentMethod.getDescription());
        p.setIsDefault(paymentMethod.getIsDefault());
        p.setIsOnline(paymentMethod.getIsOnline());
        p.setIsMobileMoney(paymentMethod.getIsMobileMoney());
        p.setInstructionUrl(paymentMethod.getInstructionUrl());
        p.setLogoUrl(paymentMethod.getLogoUrl());
        p.setWithdrawalRequirePassword(paymentMethod.getWithdrawalRequirePassword());
        p.setCreatedAt(paymentMethod.getCreatedAt());
        p.setUpdatedAt(paymentMethod.getUpdatedAt());
        return p;
    }

    public static PaymentMethodMinimalDto toMinimalDto(PaymentMethodDto paymentMethod) {
        return PaymentMethodMinimalDto.builder()
                .id(paymentMethod.getId())
                .code(paymentMethod.getCode())
                .name(paymentMethod.getName())
                .description(paymentMethod.getDescription())
                .build();
    }
}
