package com.ebingo.backend.payment.service;

import com.ebingo.backend.payment.dto.DepositTransferDto;
import com.ebingo.backend.payment.dto.DepositTransferRequestDto;
import com.ebingo.backend.payment.entity.Wallet;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface DepositTransferService {
    Flux<DepositTransferDto> getPaginatedDepositTransfer(Long telegramId, Long agentId, Integer page, Integer size, String sortBy);

    Mono<DepositTransferDto> getASingleDepositTransfer(Long id, Long telegramId, Long agentId);

    Mono<Void> deleteDepositTransfer(Long id, Long telegramId, Long agentId);

    Mono<DepositTransferDto> createDepositTransfer(@Valid DepositTransferRequestDto depositTransferDto, Long telegramId);

    Mono<Wallet> creditReceiverWalletBalanceForTransfer(Wallet receiverWallet, BigDecimal amount);
}
