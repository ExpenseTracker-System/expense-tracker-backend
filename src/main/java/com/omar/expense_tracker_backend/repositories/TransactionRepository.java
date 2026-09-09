package com.omar.expense_tracker_backend.repositories;

import com.omar.expense_tracker_backend.models.Transaction;
import com.omar.expense_tracker_backend.models.User;
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
    List<Transaction> findByWallet(Wallet wallet);
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.wallet.id= :walletId AND t.isIncome = :isIncome")
    Double calculateTotalByWalletAndType(@Param("walletId") String walletId, @Param("isIncome") Boolean isIncome);
    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findTransactionsByDateRange(
            @Param("walletId") String walletId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.wallet.id = :walletId AND t.isIncome = :isIncome AND t.transactionDate BETWEEN :startDate AND :endDate")
    Double calculateTotalByDateRange(
            @Param("walletId") String walletId,
            @Param("isIncome") Boolean isIncome,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    @Query("SELECT t.category AS category, SUM(t.amount) AS total FROM Transaction t WHERE t.wallet.id = :walletId AND t.isIncome = false GROUP BY t.category")
    List<CategoryTotalProjection> findExpenseStatsByCategory(@Param("walletId") String walletId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Transaction t SET t.isDeleted = true, t.updatedAt = :updatedAt WHERE t.id IN :ids")
    int softDeleteTransactionsInBatch(
            @Param("ids") List<String> ids,
            @Param("updatedAt") LocalDateTime updatedAt
    );
    // دالة المزامنة: تجلب كل المعاملات (حتى المحذوفة) التابعة لمحافظ المستخدم والتي تم تعديلها بعد تاريخ معين
    @Query(value = "SELECT t.* FROM transactions t JOIN wallets w ON t.wallet_id = w.id WHERE w.user_id = :userId AND t.updated_at > :lastSyncTime", nativeQuery = true)
    List<Transaction> findTransactionsForSync(
            @Param("userId") String userId,
            @Param("lastSyncTime") java.time.LocalDateTime lastSyncTime
    );
    // مكنسة تنظيف المعاملات: تحذف نهائياً أي معاملة محذوفة ناعماً ومر عليها وقت طويل
    @Modifying
    @Query(value = "DELETE FROM transactions WHERE is_deleted = true AND updated_at < :thresholdDate", nativeQuery = true)
    int hardDeleteOldTransactions(@Param("thresholdDate") java.time.LocalDateTime thresholdDate);
}
