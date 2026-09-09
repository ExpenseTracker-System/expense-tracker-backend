package com.omar.expense_tracker_backend.dtos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SyncRequest {
    // 1. العلامة الزمنية (الأهم في العملية كلها)
    private LocalDateTime lastSyncTime;
    // 2. قوائم التعديلات المحلية
    private List<WalletDTO> wallets = new ArrayList<>();
    private List<TransactionDTO> transactions = new ArrayList<>();
    public SyncRequest() {
    }

    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(LocalDateTime lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public List<WalletDTO> getWallets() {
        return wallets;
    }

    public void setWallets(List<WalletDTO> wallets) {
        this.wallets = wallets;
    }

    public List<TransactionDTO> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionDTO> transactions) {
        this.transactions = transactions;
    }
}