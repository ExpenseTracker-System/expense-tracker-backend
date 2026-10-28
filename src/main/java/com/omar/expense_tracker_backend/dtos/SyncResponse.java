package com.omar.expense_tracker_backend.dtos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SyncResponse {
    private LocalDateTime syncTime;
    private List<WalletDTO> wallets = new ArrayList<>();
    private List<TransactionDTO> transactions = new ArrayList<>();

    public SyncResponse() {}

    public LocalDateTime getSyncTime() { return syncTime; }
    public void setSyncTime(LocalDateTime syncTime) { this.syncTime = syncTime; }
    public List<WalletDTO> getWallets() { return wallets; }
    public void setWallets(List<WalletDTO> wallets) { this.wallets = wallets; }
    public List<TransactionDTO> getTransactions() { return transactions; }
    public void setTransactions(List<TransactionDTO> transactions) { this.transactions = transactions; }
}