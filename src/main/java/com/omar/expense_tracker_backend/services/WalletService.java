package com.omar.expense_tracker_backend.services;

import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.models.Wallet;
import com.omar.expense_tracker_backend.repositories.TransactionRepository;
import com.omar.expense_tracker_backend.repositories.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Wallet> getWalletsByUser(User user) {
        return walletRepository.findByUserOrderByCreatedAtAsc(user);
    }

    public Wallet createWallet(Wallet wallet) {
        if (wallet.getId() == null || wallet.getId().isBlank()) {
            wallet.setId(UUID.randomUUID().toString());
        }
        return walletRepository.save(wallet);
    }

    public Optional<Wallet> findById(String id) {
        return walletRepository.findById(id);
    }

    @Transactional
    public void softDeleteWalletWithTransactions(Wallet wallet) {
        LocalDateTime now = LocalDateTime.now();
        transactionRepository.softDeleteTransactionsByWalletId(wallet.getId(), now);
        walletRepository.softDeleteWalletById(wallet.getId(), now);
    }
}