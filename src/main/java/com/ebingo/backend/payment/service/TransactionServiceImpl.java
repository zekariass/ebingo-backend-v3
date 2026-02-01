package com.ebingo.backend.payment.service;

import com.ebingo.backend.payment.dto.TransactionDto;
import com.ebingo.backend.payment.entity.PaymentOrder;
import com.ebingo.backend.payment.entity.Transaction;
import com.ebingo.backend.payment.enums.TransactionStatus;
import com.ebingo.backend.payment.mappers.TransactionMapper;
import com.ebingo.backend.payment.repository.TransactionRepository;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final UserProfileService userProfileService;
    private final TransactionRepository transactionRepository;


    @Override
    public Flux<TransactionDto> getPaginatedTransaction(Long telegramId, Long agentId, Integer page, Integer size, String sortBy) {
        int pageNumber = (page != null && page >= 1) ? page : 1;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 10;
        long offset = (long) (pageNumber - 1) * pageSize;

        return userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User profile not found")))
                .flatMapMany(up -> {
                    String sortKey = (sortBy != null) ? sortBy.toLowerCase() : "id";
                    return switch (sortKey) {
                        case "txnamount" -> transactionRepository
                                .findByPlayerIdOrderByTxnAmountDesc(up.getId(), pageSize, offset);
                        case "createdat" -> transactionRepository
                                .findByPlayerIdOrderByCreatedAtDesc(up.getId(), pageSize, offset);
                        default -> transactionRepository
                                .findByPlayerIdOrderByIdDesc(up.getId(), pageSize, offset);
                    };
                })
                .map(TransactionMapper::toDto)
                .doOnSubscribe(s -> log.info("Fetching transactions - Page: {}, Size: {}, SortBy: {}", page, size, sortBy))
                .doOnComplete(() -> log.info("Completed fetching transactions for user: {}", telegramId))
                .doOnError(e -> log.error("Failed to fetch transactions: {}", e.getMessage(), e));
    }

    private String generateTxnRef() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    @Override
    public Mono<TransactionDto> createTransaction(PaymentOrder order) {
        Transaction transaction = new Transaction();
        transaction.setTxnRef(generateTxnRef());
        transaction.setPaymentMethodId(order.getPaymentMethodId());
        transaction.setPlayerId(order.getUserId());
        transaction.setTxnType(order.getTxnType());
        transaction.setStatus(TransactionStatus.COMPLETED); // ensure this is intended
        transaction.setOrderId(order.getId());
        transaction.setTxnAmount(order.getAmount());

        return transactionRepository.save(transaction)
                .map(TransactionMapper::toDto)
                .doOnSubscribe(s -> log.info("Creating transaction for order with id: {}", order.getId()))
                .doOnSuccess(tx -> log.info("Transaction created successfully: {}", tx))
                .doOnError(e -> log.error("Failed to create transaction: {}", e.getMessage(), e));
    }

}
