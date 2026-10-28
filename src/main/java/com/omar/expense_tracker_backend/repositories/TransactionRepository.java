package com.omar.expense_tracker_backend.repositories;

import com.omar.expense_tracker_backend.models.Transaction;
import com.omar.expense_tracker_backend.models.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByWalletOrderByTransactionDateDesc(Wallet wallet);

    @Query(value = "SELECT * FROM transactions WHERE id = :id", nativeQuery = true)
    Optional<Transaction> findByIdIncludingDeleted(@Param("id") String id);

    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM Transaction t WHERE t.wallet.id = :walletId AND t.isIncome = :isIncome")
    Double calculateTotalByWalletAndType(@Param("walletId") String walletId, @Param("isIncome") Boolean isIncome);

    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findTransactionsByDateRange(
            @Param("walletId") String walletId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId " +
            "AND (LOWER(t.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(COALESCE(t.note, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> searchAndFilterTransactions(
            @Param("walletId") String walletId,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM Transaction t WHERE t.wallet.id = :walletId AND t.isIncome = :isIncome AND t.transactionDate BETWEEN :startDate AND :endDate")
    Double calculateTotalByDateRange(
            @Param("walletId") String walletId,
            @Param("isIncome") Boolean isIncome,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t.category AS category, COALESCE(SUM(t.amount), 0.0) AS total FROM Transaction t WHERE t.wallet.id = :walletId AND t.isIncome = false GROUP BY t.category ORDER BY total DESC")
    List<CategoryTotalProjection> findExpenseStatsByCategory(@Param("walletId") String walletId);

    @Modifying
    @Query("UPDATE Transaction t SET t.isDeleted = true, t.updatedAt = :updatedAt WHERE t.id IN :ids AND t.wallet.user.email = :userEmail")
    int softDeleteTransactionsInBatch(
            @Param("ids") List<String> ids,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("userEmail") String userEmail
    );

    @Modifying
    @Query("UPDATE Transaction t SET t.isDeleted = true, t.updatedAt = :updatedAt WHERE t.wallet.id = :walletId")
    int softDeleteTransactionsByWalletId(
            @Param("walletId") String walletId,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Query(value = "SELECT t.* FROM transactions t JOIN wallets w ON t.wallet_id = w.id WHERE w.user_id = :userId AND t.updated_at > :lastSyncTime", nativeQuery = true)
    List<Transaction> findTransactionsForSync(
            @Param("userId") String userId,
            @Param("lastSyncTime") LocalDateTime lastSyncTime
    );

    @Modifying
    @Query(value = "DELETE FROM transactions WHERE (is_deleted = true AND updated_at < :thresholdDate) OR wallet_id IN (SELECT id FROM wallets WHERE is_deleted = true AND updated_at < :thresholdDate)", nativeQuery = true)
    int hardDeleteOldTransactions(@Param("thresholdDate") LocalDateTime thresholdDate);
}