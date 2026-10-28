package com.omar.expense_tracker_backend.controllers;

import com.omar.expense_tracker_backend.dtos.TransactionDTO;
import com.omar.expense_tracker_backend.exceptions.ResourceNotFoundException;
import com.omar.expense_tracker_backend.models.Transaction;
import com.omar.expense_tracker_backend.models.Wallet;
import com.omar.expense_tracker_backend.responses.ApiResponse;
import com.omar.expense_tracker_backend.services.TransactionService;
import com.omar.expense_tracker_backend.services.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    private Wallet getAuthorizedWallet(String walletId, Authentication auth) {
        Wallet wallet = walletService.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("المحفظة غير موجودة"));
        if (!wallet.getUser().getEmail().equals(auth.getName())) {
            throw new IllegalArgumentException("غير مصرح لك بالوصول إلى معاملات هذه المحفظة");
        }
        return wallet;
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByWallet(
            @PathVariable String walletId, Authentication auth) {
        Wallet wallet = getAuthorizedWallet(walletId, auth);
        List<TransactionDTO> dtos = transactionService.findByWallet(wallet).stream()
                .map(t -> new TransactionDTO(
                        t.getId(), t.getAmount(), t.getCategory(), t.getNote(),
                        t.getIncome(), t.getTransactionDate(), wallet.getId()
                )).toList();
        return ResponseEntity.ok(new ApiResponse<>("تم جلب المعاملات بنجاح", dtos));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionDTO>> createTransaction(
            @Valid @RequestBody TransactionDTO transactionDto, Authentication auth) {
        Wallet wallet = getAuthorizedWallet(transactionDto.getWalletId(), auth);

        Transaction transaction = new Transaction();
        transaction.setId(transactionDto.getId());
        transaction.setCategory(transactionDto.getCategory());
        transaction.setAmount(transactionDto.getAmount());
        transaction.setIncome(transactionDto.getIncome());
        transaction.setNote(transactionDto.getNote());
        transaction.setTransactionDate(transactionDto.getTransactionDate());
        transaction.setWallet(wallet);

        Transaction savedTransaction = transactionService.createTransaction(transaction);
        TransactionDTO responseDto = new TransactionDTO(
                savedTransaction.getId(),
                savedTransaction.getAmount(),
                savedTransaction.getCategory(),
                savedTransaction.getNote(),
                savedTransaction.getIncome(),
                savedTransaction.getTransactionDate(),
                savedTransaction.getWallet().getId()
        );

        return ResponseEntity.ok(new ApiResponse<>("تم إنشاء المعاملة بنجاح", responseDto));
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionDTO>> updateTransaction(
            @PathVariable String transactionId,
            @Valid @RequestBody TransactionDTO transactionDto,
            Authentication auth) {
        Transaction updatedData = new Transaction();
        updatedData.setAmount(transactionDto.getAmount());
        updatedData.setCategory(transactionDto.getCategory());
        updatedData.setNote(transactionDto.getNote());
        updatedData.setIncome(transactionDto.getIncome());
        updatedData.setTransactionDate(transactionDto.getTransactionDate());

        Transaction saved = transactionService.updateTransaction(transactionId, updatedData, auth.getName());
        TransactionDTO responseDto = new TransactionDTO(
                saved.getId(), saved.getAmount(), saved.getCategory(), saved.getNote(),
                saved.getIncome(), saved.getTransactionDate(), saved.getWallet().getId()
        );

        return ResponseEntity.ok(new ApiResponse<>("تم تحديث المعاملة بنجاح", responseDto));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Integer>> deleteTransactionsInBatch(
            @RequestBody List<String> ids, Authentication auth) {
        int deletedCount = transactionService.deleteTransactionsInBatch(ids, auth.getName());
        return ResponseEntity.ok(new ApiResponse<>("تم حذف " + deletedCount + " معاملة بنجاح", deletedCount));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByDateRange(
            @RequestParam String walletId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String keyword,
            Authentication auth) {
        getAuthorizedWallet(walletId, auth);

        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        List<Transaction> transactions = transactionService.findTransactionsByDateRange(walletId, keyword, start, end);
        List<TransactionDTO> dtos = transactions.stream().map(t -> new TransactionDTO(
                t.getId(), t.getAmount(), t.getCategory(), t.getNote(),
                t.getIncome(), t.getTransactionDate(), t.getWallet().getId()
        )).toList();

        return ResponseEntity.ok(new ApiResponse<>("تم جلب المعاملات المفلترة بنجاح", dtos));
    }
}