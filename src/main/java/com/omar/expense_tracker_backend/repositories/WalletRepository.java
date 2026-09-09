package com.omar.expense_tracker_backend.repositories;

import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.models.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {
    List<Wallet> findByUser(User user);
    @Query(value = "SELECT * FROM wallets WHERE user_id = :userId AND updated_at > :lastSyncTime", nativeQuery = true)
    List<Wallet> findWalletsForSync(
            @Param("userId") String userId,
            @Param("lastSyncTime") java.time.LocalDateTime lastSyncTime
    );
    // مكنسة تنظيف المحافظ: تحذف نهائياً أي محفظة محذوفة ناعماً ومر عليها وقت طويل
    @Modifying
    @Query(value = "DELETE FROM wallets WHERE is_deleted = true AND updated_at < :thresholdDate", nativeQuery = true)
    int hardDeleteOldWallets(@Param("thresholdDate") java.time.LocalDateTime thresholdDate);
}
