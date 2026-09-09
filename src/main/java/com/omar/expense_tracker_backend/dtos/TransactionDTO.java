package com.omar.expense_tracker_backend.dtos;

import java.time.LocalDateTime;

public class TransactionDTO {
    private String id;
    private Double amount;
    private String category;
    private String note;
    private Boolean isIncome;
    private LocalDateTime transactionsDate;
    private String walletId;
    private Boolean isDeleted = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public TransactionDTO(String id, Double amount, String category, String note, Boolean isIncome, LocalDateTime transactionsDate, String walletId) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.isIncome = isIncome;
        this.transactionsDate = transactionsDate;
        this.walletId = walletId;
    }
    public TransactionDTO() {

    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getIncome() {
        return isIncome;
    }

    public void setIncome(Boolean income) {
        isIncome = income;
    }

    public LocalDateTime getTransactionsDate() {
        return transactionsDate;
    }

    public void setTransactionsDate(LocalDateTime transactionsDate) {
        this.transactionsDate = transactionsDate;
    }
}
