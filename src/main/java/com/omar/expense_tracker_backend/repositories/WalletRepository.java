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

    @Modifying
    @Query(value = "INSERT INTO wallets (id, name, user_id, is_deleted, created_at, updated_at) " +
            "VALUES (:id, :name, :userId, :isDeleted, :createdAt, :updatedAt) " +
            "ON CONFLICT (id) DO UPDATE SET " +
            "name = EXCLUDED.name, " +
            "is_deleted = EXCLUDED.is_deleted, " +
            "updated_at = EXCLUDED.updated_at", nativeQuery = true)
    void upsertWallet(
            @Param("id") String id,
            @Param("name") String name,
            @Param("userId") String userId,
            @Param("isDeleted") boolean isDeleted,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );

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