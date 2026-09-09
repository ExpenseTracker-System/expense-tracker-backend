package com.omar.expense_tracker_backend.services;
import com.omar.expense_tracker_backend.models.Transaction;
import com.omar.expense_tracker_backend.models.Wallet;
import com.omar.expense_tracker_backend.repositories.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import com.omar.expense_tracker_backend.repositories.CategoryTotalProjection;
@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    public List<Transaction>  findByWallet(Wallet wallet) {

        return transactionRepository.findByWallet(wallet);
    }
    public Transaction createTransaction(Transaction transaction) {
        if (transaction.getAmount()<=0) {
            throw new IllegalArgumentException("المبلغ لا يمكن ان يكون  سالبا او صفر");
        }
        return transactionRepository.save(transaction);
    }
    public Double calculateTotalByWalletAndType(String walletId, Boolean isIncome) {
        Double result = transactionRepository.calculateTotalByWalletAndType(walletId, isIncome);
        return (result == null) ? 0.0 : result;
    }
    // 1. جلب المعاملات محصورة بين تاريخين
    public List<Transaction> findTransactionsByDateRange(String walletId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findTransactionsByDateRange(walletId, startDate, endDate);
    }
    // 2. حساب الإجمالي محصوراً بين تاريخين
    public Double calculateTotalByDateRange(String walletId, Boolean isIncome, LocalDateTime startDate, LocalDateTime endDate) {
        Double result = transactionRepository.calculateTotalByDateRange(walletId, isIncome, startDate, endDate);
        return (result == null) ? 0.0 : result;
    }
    // 3. جلب إحصائيات المصروفات مجمعة بالفئات
    public List<CategoryTotalProjection> getExpenseStatsByCategory(String walletId) {
        return transactionRepository.findExpenseStatsByCategory(walletId);
    }
    // 4. دالة الحذف المتعدد (Batch Delete)
    @org.springframework.transaction.annotation.Transactional
    public int deleteTransactionsInBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        // نمرر الوقت الحالي كطابع زمني لعملية الحذف لضمان المزامنة
        return transactionRepository.softDeleteTransactionsInBatch(ids, LocalDateTime.now());
    }

}
