package com.omar.expense_tracker_backend.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public class TransactionDTO {
    private String id;

    @NotNull(message = "المبلغ مطلوب")
    @Positive(message = "المبلغ يجب أن يكون أكبر من الصفر")
    private Double amount;

    @NotBlank(message = "الفئة مطلوبة")
    private String category;

    private String note;

    @NotNull(message = "نوع المعاملة (إيراد/مصروف) مطلوب")
    @JsonProperty("isIncome")
    @JsonAlias({"income", "isIncome"})
    private Boolean isIncome;

    @NotNull(message = "تاريخ المعاملة مطلوب")
    @JsonProperty("transactionDate")
    @JsonAlias({"transactionsDate", "transactionDate"})
    private LocalDateTime transactionDate;

    @NotBlank(message = "معرف المحفظة مطلوب")
    private String walletId;

    @JsonProperty("isDeleted")
    @JsonAlias({"deleted", "isDeleted"})
    private Boolean isDeleted = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TransactionDTO() {}

    public TransactionDTO(String id, Double amount, String category, String note, Boolean isIncome, LocalDateTime transactionDate, String walletId) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.isIncome = isIncome;
        this.transactionDate = transactionDate;
        this.walletId = walletId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Boolean getIncome() { return isIncome; }
    public void setIncome(Boolean income) { this.isIncome = income; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public LocalDateTime getTransactionsDate() { return transactionDate; }
    public void setTransactionsDate(LocalDateTime transactionsDate) { this.transactionDate = transactionsDate; }
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    public Boolean getDeleted() { return isDeleted; }
    public void setDeleted(Boolean deleted) { this.isDeleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}