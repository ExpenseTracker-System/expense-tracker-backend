package com.omar.expense_tracker_backend.services;

import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.models.Wallet;
import com.omar.expense_tracker_backend.repositories.WalletRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }
    public List<Wallet> getWalletsByUser(User user) {
        return walletRepository.findByUser(user);
    }
    public Wallet createWallet(Wallet wallet) {
        return walletRepository.save(wallet);
    }
    public Optional<Wallet> findById(String id) {
        return walletRepository.findById(id);
    }
}
