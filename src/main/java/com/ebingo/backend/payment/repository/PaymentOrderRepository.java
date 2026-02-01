package com.ebingo.backend.payment.repository;


import com.ebingo.backend.payment.entity.PaymentOrder;
import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.ebingo.backend.payment.enums.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentOrderRepository extends ReactiveCrudRepository<PaymentOrder, Long> {
    Mono<PaymentOrder> findByTxnRef(String txnRef);

    Mono<PaymentOrder> findByProviderOrderRef(String providerOrderRef);

    @Query("SELECT * FROM payment_order WHERE user_id = :userId ORDER BY created_at DESC LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<PaymentOrder> findByUserIdPaged(Long userId, Pageable pageable);

    @Query("SELECT * FROM payment_order WHERE payment_method = :method AND status = :status ORDER BY created_at DESC LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<PaymentOrder> findByMethodAndStatusPaged(String method, String status, Pageable pageable);

    // 🔹 OFFLINE ORDERS (provider_order_ref IS NULL)
    @Query("SELECT * FROM payment_order WHERE provider_order_ref IS NULL ORDER BY amount DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findOfflineOrdersOrderByAmountDesc(int limit, long offset);

    @Query("SELECT * FROM payment_order WHERE provider_order_ref IS NULL ORDER BY status DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findOfflineOrdersOrderByStatusDesc(int limit, long offset);

    @Query("SELECT * FROM payment_order WHERE provider_order_ref IS NULL ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findOfflineOrdersOrderByUpdatedAtDesc(int limit, long offset);

    @Query("SELECT * FROM payment_order WHERE provider_order_ref IS NULL ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findOfflineOrdersOrderByCreatedAtDesc(int limit, long offset);

    @Query("SELECT * FROM payment_order WHERE provider_order_ref IS NULL ORDER BY id DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findOfflineOrdersOrderByIdDesc(int limit, long offset);


    // 🔹 OFFLINE ORDERS BY STATUS
    @Query("SELECT * FROM payment_order WHERE provider_order_ref IS NULL AND status = :status ORDER BY amount DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findOfflineOrdersByStatusOrderByAmountDesc(String status, int limit, long offset);

    @Query("SELECT * FROM payment_order WHERE provider_order_ref IS NULL AND status = :status ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findOfflineOrdersByStatusOrderByUpdatedAtDesc(String status, int limit, long offset);

    @Query("SELECT * FROM payment_order WHERE provider_order_ref IS NULL AND status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findOfflineOrdersByStatusOrderByCreatedAtDesc(String status, int limit, long offset);

    @Query("SELECT * FROM payment_order WHERE provider_order_ref IS NULL AND status = :status ORDER BY id DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findOfflineOrdersByStatusOrderByIdDesc(String status, int limit, long offset);


    // 🔹 USER’S PENDING ORDERS
    @Query("SELECT * FROM payment_order WHERE user_id = :userId AND status = 'PENDING' ORDER BY amount DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findPendingOrdersByUserIdOrderByAmountDesc(long userId, int limit, long offset);

    @Query("SELECT * FROM payment_order WHERE user_id = :userId AND status = 'PENDING' ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findPendingOrdersByUserIdOrderByCreatedAtDesc(long userId, int limit, long offset);

    @Query("SELECT * FROM payment_order WHERE user_id = :userId AND status = 'PENDING' ORDER BY id DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findPendingOrdersByUserIdOrderByIdDesc(long userId, int limit, long offset);


    @Query("SELECT * FROM payment_order WHERE txn_type = 'WITHDRAWAL' ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findAllWithdrawals(int limit, int offset);

    @Query("SELECT * FROM payment_order WHERE txn_type = 'WITHDRAWAL' AND status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findAllWithdrawalsByStatus(String status, int limit, int offset);

    @Query("SELECT * FROM payment_order WHERE user_id = :userId AND txn_type = 'WITHDRAWAL' ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findWithdrawalsByUser(Long userId, int limit, int offset);

    @Query("SELECT * FROM payment_order WHERE user_id = :userId AND txn_type = 'WITHDRAWAL' AND status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findWithdrawalsByUserAndStatus(Long userId, String status, int limit, int offset);

    @Query("SELECT COUNT(*) FROM payment_order WHERE txn_type = 'WITHDRAWAL'")
    Mono<Long> countAllWithdrawals();

    @Query("SELECT COUNT(*) FROM payment_order WHERE txn_type = 'WITHDRAWAL' AND status = :status")
    Mono<Long> countAllWithdrawalsByStatus(String status);

    @Query("SELECT COUNT(*) FROM payment_order WHERE user_id = :userId AND txn_type = 'WITHDRAWAL'")
    Mono<Long> countUserWithdrawals(Long userId);

    @Query("SELECT COUNT(*) FROM payment_order WHERE user_id = :userId AND txn_type = 'WITHDRAWAL' AND status = :status")
    Mono<Long> countUserWithdrawalsByStatus(Long userId, String status);


    @Query("SELECT * FROM payment_order WHERE txn_type = 'DEPOSIT' ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findAllDeposits(int limit, int offset);

    @Query("SELECT * FROM payment_order WHERE txn_type = 'DEPOSIT' AND status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findAllDepositsByStatus(String status, int limit, int offset);

    @Query("SELECT * FROM payment_order WHERE user_id = :userId AND txn_type = 'DEPOSIT' ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findDepositsByUser(Long userId, int limit, int offset);

    @Query("SELECT * FROM payment_order WHERE user_id = :userId AND txn_type = 'DEPOSIT' AND status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<PaymentOrder> findDepositsByUserAndStatus(Long userId, String status, int limit, int offset);

    @Query("SELECT COUNT(*) FROM payment_order WHERE txn_type = 'DEPOSIT'")
    Mono<Long> countAllDeposits();

    @Query("SELECT COUNT(*) FROM payment_order WHERE txn_type = 'DEPOSIT' AND status = :status")
    Mono<Long> countAllDepositsByStatus(String status);

    @Query("SELECT COUNT(*) FROM payment_order WHERE user_id = :userId AND txn_type = 'DEPOSIT'")
    Mono<Long> countUserDeposits(Long userId);

    @Query("SELECT COUNT(*) FROM payment_order WHERE user_id = :userId AND txn_type = 'DEPOSIT' AND status = :status")
    Mono<Long> countUserDepositsByStatus(Long userId, String status);

    @Query("""
                SELECT * FROM payment_order
                WHERE phone_number = :phone
                  AND agent_id = :agentId
                  AND (:status IS NULL OR status = :status)
                  AND (:txnType IS NULL OR txn_type = :txnType)
                ORDER BY id DESC
                LIMIT :limit OFFSET :offset
            """)
    Flux<PaymentOrder> findByUserPhoneAndAgentIdOrderedDesc(
            String phone,
            Long agentId,
            PaymentOrderStatus status,
            TransactionType txnType,
            int limit,
            long offset
    );


    @Query("""
                SELECT * FROM payment_order
                WHERE (:status IS NULL OR status = :status)
                  AND (:txnType IS NULL OR txn_type = :txnType)
                  AND agent_id = :agentId
                ORDER BY id DESC
                LIMIT :limit OFFSET :offset
            """)
    Flux<PaymentOrder> findAllWithFiltersOrderedDesc(
            Long agentId,
            PaymentOrderStatus status,
            TransactionType txnType,
            int limit,
            long offset
    );


    @Query("""
                SELECT COUNT(*) FROM payment_order
                WHERE phone_number = :phone
                  AND agent_id = :agentId
                  AND (:status IS NULL OR status = :status)
                  AND (:txnType IS NULL OR txn_type = :txnType)
            """)
    Mono<Long> countByUserPhoneAndAgentId(
            String phone,
            Long agentId,
            PaymentOrderStatus status,
            TransactionType txnType
    );


    @Query("""
                SELECT COUNT(*) FROM payment_order
                WHERE (:status IS NULL OR status = :status)
                  AND (:txnType IS NULL OR txn_type = :txnType)
                    AND agent_id = :agentId
            """)
    Mono<Long> countAllWithFilters(
            Long agentId, PaymentOrderStatus status,
            TransactionType txnType
    );


    @Query("SELECT * FROM payment_order WHERE provider_order_ref = :paymentProviderRef AND status = :paymentOrderStatus AND agent_id = :agentId")
    Mono<PaymentOrder> findByProviderOrderRefAndStatusAndAgentId(String paymentProviderRef, PaymentOrderStatus paymentOrderStatus, Long agentId);
}

