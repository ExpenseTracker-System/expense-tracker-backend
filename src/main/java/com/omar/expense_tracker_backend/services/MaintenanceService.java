package com.omar.expense_tracker_backend.services;

import com.omar.expense_tracker_backend.repositories.TransactionRepository;
import com.omar.expense_tracker_backend.repositories.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public MaintenanceService(TransactionRepository transactionRepository, WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    @Scheduled(cron = "0 0 3 * * FRI")
    @Transactional
    public void cleanUpDeletedData() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(30);

        int deletedTransactionsCount = transactionRepository.hardDeleteOldTransactions(thresholdDate);
        int deletedWalletsCount = walletRepository.hardDeleteOldWallets(thresholdDate);

        log.info("🧹 [Maintenance Robot] Cleanup completed at: {}", LocalDateTime.now());
        log.info("   - Deleted old transactions: {}", deletedTransactionsCount);
        log.info("   - Deleted old wallets: {}", deletedWalletsCount);
    }
}