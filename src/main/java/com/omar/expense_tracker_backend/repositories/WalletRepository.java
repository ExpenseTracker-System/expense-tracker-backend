package com.omar.expense_tracker_backend.repositories;

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
public interface WalletRepository extends JpaRepository<Wallet, String> {
    List<Wallet> findByUserOrderByCreatedAtAsc(User user);

    @Query(value = "SELECT * FROM wallets WHERE id = :id", nativeQuery = true)
    Optional<Wallet> findByIdIncludingDeleted(@Param("id") String id);

    @Query(value = "SELECT * FROM wallets WHERE user_id = :userId AND updated_at > :lastSyncTime", nativeQuery = true)
    List<Wallet> findWalletsForSync(
            @Param("userId") String userId,
            @Param("lastSyncTime") LocalDateTime lastSyncTime
    );

    @Modifying
    @Query("UPDATE Wallet w SET w.isDeleted = true, w.updatedAt = :updatedAt WHERE w.id = :walletId")
    int softDeleteWalletById(@Param("walletId") String walletId, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query(value = "DELETE FROM wallets WHERE is_deleted = true AND updated_at < :thresholdDate", nativeQuery = true)
    int hardDeleteOldWallets(@Param("thresholdDate") LocalDateTime thresholdDate);
}