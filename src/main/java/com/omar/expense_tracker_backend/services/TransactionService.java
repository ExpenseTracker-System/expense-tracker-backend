package com.omar.expense_tracker_backend.services;

import com.omar.expense_tracker_backend.exceptions.ResourceNotFoundException;
import com.omar.expense_tracker_backend.models.Transaction;
import com.omar.expense_tracker_backend.models.Wallet;
import com.omar.expense_tracker_backend.repositories.CategoryTotalProjection;
import com.omar.expense_tracker_backend.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> findByWallet(Wallet wallet) {
        return transactionRepository.findByWalletOrderByTransactionDateDesc(wallet);
    }

    public Optional<Transaction> findById(String id) {
        return transactionRepository.findById(id);
    }

    public Transaction createTransaction(Transaction transaction) {
        if (transaction.getAmount() == null || transaction.getAmount() <= 0) {
            throw new IllegalArgumentException("المبلغ لا يمكن أن يكون سالباً أو صفراً");
        }
        if (transaction.getId() == null || transaction.getId().isBlank()) {
            transaction.setId(java.util.UUID.randomUUID().toString());
        }
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction updateTransaction(String transactionId, Transaction updatedData, String userEmail) {
        Transaction existing = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("المعاملة غير موجودة"));

        if (!existing.getWallet().getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("غير مصرح لك بتعديل هذه المعاملة");
        }
        if (updatedData.getAmount() == null || updatedData.getAmount() <= 0) {
            throw new IllegalArgumentException("المبلغ لا يمكن أن يكون سالباً أو صفراً");
        }

        existing.setAmount(updatedData.getAmount());
        existing.setCategory(updatedData.getCategory());
        existing.setNote(updatedData.getNote());
        existing.setIncome(updatedData.getIncome());
        existing.setTransactionDate(updatedData.getTransactionDate());

        return transactionRepository.save(existing);
    }

    public Double calculateTotalByWalletAndType(String walletId, Boolean isIncome) {
        Double result = transactionRepository.calculateTotalByWalletAndType(walletId, isIncome);
        return (result == null) ? 0.0 : result;
    }

    public List<Transaction> findTransactionsByDateRange(String walletId, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return transactionRepository.searchAndFilterTransactions(walletId, keyword.trim(), startDate, endDate);
        }
        return transactionRepository.findTransactionsByDateRange(walletId, startDate, endDate);
    }

    public Double calculateTotalByDateRange(String walletId, Boolean isIncome, LocalDateTime startDate, LocalDateTime endDate) {
        Double result = transactionRepository.calculateTotalByDateRange(walletId, isIncome, startDate, endDate);
        return (result == null) ? 0.0 : result;
    }

    public List<CategoryTotalProjection> getExpenseStatsByCategory(String walletId) {
        return transactionRepository.findExpenseStatsByCategory(walletId);
    }

    @Transactional
    public int deleteTransactionsInBatch(List<String> ids, String userEmail) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return transactionRepository.softDeleteTransactionsInBatch(ids, LocalDateTime.now(), userEmail);
    }
}