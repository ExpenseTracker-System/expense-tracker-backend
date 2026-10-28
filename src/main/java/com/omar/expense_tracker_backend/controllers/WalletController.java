package com.omar.expense_tracker_backend.controllers;

import com.omar.expense_tracker_backend.dtos.WalletDTO;
import com.omar.expense_tracker_backend.dtos.WalletStatsDTO;
import com.omar.expense_tracker_backend.exceptions.ResourceNotFoundException;
import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.models.Wallet;
import com.omar.expense_tracker_backend.repositories.CategoryTotalProjection;
import com.omar.expense_tracker_backend.responses.ApiResponse;
import com.omar.expense_tracker_backend.services.TransactionService;
import com.omar.expense_tracker_backend.services.UserService;
import com.omar.expense_tracker_backend.services.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;
    private final TransactionService transactionService;

    public WalletController(WalletService walletService, UserService userService, TransactionService transactionService) {
        this.walletService = walletService;
        this.userService = userService;
        this.transactionService = transactionService;
    }

    private Wallet getAuthorizedWallet(String walletId, Authentication auth) {
        Wallet wallet = walletService.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("المحفظة غير موجودة"));
        if (!wallet.getUser().getEmail().equals(auth.getName())) {
            throw new IllegalArgumentException("غير مصرح لك بالوصول إلى هذه المحفظة");
        }
        return wallet;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WalletDTO>>> getUserWallets(Authentication auth) {
        User user = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود"));
        List<WalletDTO> wallets = walletService.getWalletsByUser(user).stream()
                .map(w -> new WalletDTO(w.getId(), w.getName(), user.getId()))
                .toList();
        return ResponseEntity.ok(new ApiResponse<>("تم جلب المحافظ بنجاح", wallets));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WalletDTO>> createWallet(
            @Valid @RequestBody WalletDTO walletDto, Authentication auth) {
        User user = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم صاحب المحفظة غير موجود"));

        Wallet wallet = new Wallet();
        wallet.setId(walletDto.getId());
        wallet.setName(walletDto.getName());
        wallet.setUser(user);

        Wallet savedWallet = walletService.createWallet(wallet);
        WalletDTO responseDto = new WalletDTO(
                savedWallet.getId(),
                savedWallet.getName(),
                savedWallet.getUser().getId()
        );

        return ResponseEntity.ok(new ApiResponse<>("تم إنشاء المحفظة بنجاح", responseDto));
    }

    @PutMapping("/{walletId}")
    public ResponseEntity<ApiResponse<WalletDTO>> updateWallet(
            @PathVariable String walletId,
            @Valid @RequestBody WalletDTO walletDto,
            Authentication auth) {
        Wallet wallet = getAuthorizedWallet(walletId, auth);
        wallet.setName(walletDto.getName());
        Wallet updatedWallet = walletService.createWallet(wallet);

        WalletDTO responseDto = new WalletDTO(
                updatedWallet.getId(),
                updatedWallet.getName(),
                updatedWallet.getUser().getId()
        );
        return ResponseEntity.ok(new ApiResponse<>("تم تحديث المحفظة بنجاح", responseDto));
    }

    @DeleteMapping("/{walletId}")
    public ResponseEntity<ApiResponse<Object>> deleteWallet(@PathVariable String walletId, Authentication auth) {
        Wallet wallet = getAuthorizedWallet(walletId, auth);
        walletService.softDeleteWalletWithTransactions(wallet);
        return ResponseEntity.ok(new ApiResponse<>(true, "تم حذف المحفظة وجميع معاملاتها بنجاح", null));
    }

    @GetMapping("/{walletId}/stats")
    public ResponseEntity<ApiResponse<WalletStatsDTO>> getWalletStats(
            @PathVariable("walletId") String walletId, Authentication auth) {
        getAuthorizedWallet(walletId, auth);
        Double totalIncome = transactionService.calculateTotalByWalletAndType(walletId, true);
        Double totalExpense = transactionService.calculateTotalByWalletAndType(walletId, false);
        Double balance = totalIncome - totalExpense;

        WalletStatsDTO statsDto = new WalletStatsDTO(totalIncome, totalExpense, balance);
        return ResponseEntity.ok(new ApiResponse<>("تم جلب الإحصائيات بنجاح", statsDto));
    }

    @GetMapping("/{walletId}/stats/categories")
    public ResponseEntity<ApiResponse<List<CategoryTotalProjection>>> getExpenseStatsByCategory(
            @PathVariable String walletId, Authentication auth) {
        getAuthorizedWallet(walletId, auth);
        List<CategoryTotalProjection> stats = transactionService.getExpenseStatsByCategory(walletId);
        return ResponseEntity.ok(new ApiResponse<>("تم جلب إحصائيات الفئات بنجاح", stats));
    }
}