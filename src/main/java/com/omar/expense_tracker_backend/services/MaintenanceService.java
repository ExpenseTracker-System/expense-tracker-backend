package com.omar.expense_tracker_backend.services;

import com.omar.expense_tracker_backend.repositories.TransactionRepository;
import com.omar.expense_tracker_backend.repositories.WalletRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MaintenanceService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public MaintenanceService(TransactionRepository transactionRepository, WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    /**
     * روبوت التنظيف: يعمل في الخلفية تلقائياً
     * cron = "0 0 3 * * FRI" -> يعني كل يوم جمعة (FRI) الساعة 03:00 صباحاً (0 3).
     */
    @Scheduled(cron = "0 0 3 * * FRI")
    @Transactional
    public void cleanUpDeletedData() {
        // 1. تحديد فترة السماح (مثلاً 30 يوماً).
        // أي شيء حُذف منذ أكثر من 30 يوماً يعتبر قديماً ويمكن التخلص منه.
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(30);

        // 2. تشغيل المكانس لجمع القمامة (Hard Delete)
        int deletedTransactionsCount = transactionRepository.hardDeleteOldTransactions(thresholdDate);
        int deletedWalletsCount = walletRepository.hardDeleteOldWallets(thresholdDate);

        // 3. طباعة تقرير في شاشة الأوامر (Console) للتأكد من عمل الروبوت
        System.out.println("🧹 [Maintenance Robot] Cleanup completed at: " + LocalDateTime.now());
        System.out.println("   - Deleted old transactions: " + deletedTransactionsCount);
        System.out.println("   - Deleted old wallets: " + deletedWalletsCount);
    }
}