package com.omar.expense_tracker_backend.controllers;

import com.omar.expense_tracker_backend.dtos.TransactionDTO;
import com.omar.expense_tracker_backend.exceptions.ResourceNotFoundException;
import com.omar.expense_tracker_backend.models.Transaction;
import com.omar.expense_tracker_backend.models.Wallet;
import com.omar.expense_tracker_backend.responses.ApiResponse;
import com.omar.expense_tracker_backend.services.TransactionService;
import com.omar.expense_tracker_backend.services.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final WalletService walletService;
    public TransactionController(TransactionService transactionService, WalletService walletService) {
        this.transactionService = transactionService;
        this.walletService = walletService;
    }
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionDTO>>  createTransaction(@RequestBody TransactionDTO transactionDto) {
        Wallet wallet = walletService.findById(transactionDto.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("المحفظة غير موجودة"));
        Transaction transaction = new Transaction();
        transaction.setId(transactionDto.getId());
        transaction.setCategory(transactionDto.getCategory());
        transaction.setAmount(transactionDto.getAmount());
        transaction.setIncome(transactionDto.getIncome());
        transaction.setNote(transactionDto.getNote());
        transaction.setTransactionDate(transactionDto.getTransactionsDate());
        transaction.setWallet(wallet);
        Transaction savedTransaction=transactionService.createTransaction(transaction);
        TransactionDTO responseDto= new TransactionDTO(
                savedTransaction.getId(),
                savedTransaction.getAmount(),
                savedTransaction.getCategory(),
                savedTransaction.getNote(),
                savedTransaction.getIncome(),
                savedTransaction.getTransactionDate(),
                savedTransaction.getWallet().getId()
        );
        ApiResponse<TransactionDTO> response =new ApiResponse<>("تم انشاء المعاملة بنجاح", responseDto);
        return ResponseEntity.ok().body(response);
    }
    // 1. مسار الحذف المتعدد (Batch Delete)
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Integer>> deleteTransactionsInBatch(@RequestBody List<String> ids) {
        int deletedCount = transactionService.deleteTransactionsInBatch(ids);
        ApiResponse<Integer> response = new ApiResponse<>("تم حذف " + deletedCount + " معاملة بنجاح", deletedCount);
        return ResponseEntity.ok(response);
    }
    // 2. مسار جلب المعاملات مفلترة بين تاريخين (اختياري لفلاتر)
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByDateRange(
            @RequestParam String walletId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        // تحويل النصوص القادمة من فلاتر إلى تواريخ تفهمها الجافا
        java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
        java.time.LocalDateTime end = java.time.LocalDateTime.parse(endDate);
        List<Transaction> transactions = transactionService.findTransactionsByDateRange(walletId, start, end);
        // تحويل النماذج إلى DTOs (سنقوم بعمل حلقة تكرار بسيطة هنا أو نستخدم Map)
        List<TransactionDTO> dtos = transactions.stream().map(t -> new TransactionDTO(
                t.getId(), t.getAmount(), t.getCategory(), t.getNote(), t.getIncome(), t.getTransactionDate(), t.getWallet().getId()
        )).toList();
        ApiResponse<List<TransactionDTO>> response = new ApiResponse<>("تم جلب المعاملات المفلترة بنجاح", dtos);
        return ResponseEntity.ok(response);
    }
}
