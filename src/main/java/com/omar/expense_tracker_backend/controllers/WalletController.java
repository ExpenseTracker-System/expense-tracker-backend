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
import org.springframework.http.ResponseEntity;
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
    @PostMapping
    public ResponseEntity<ApiResponse<WalletDTO>> createWallet(@RequestBody WalletDTO walletDto) {
     User user =userService.findById(walletDto.getUserId())
             .orElseThrow(() -> new ResourceNotFoundException("المستخدم صاحب المحفظة غير موجود"));
     Wallet wallet = new Wallet();
     wallet.setId(walletDto.getId());
     wallet.setName(walletDto.getName());
     wallet.setUser(user);
     Wallet savedWallet = walletService.createWallet(wallet);
     WalletDTO responseDto =new WalletDTO(
             savedWallet.getId(),
             savedWallet.getName(),
             savedWallet.getUser().getId()
     );
     ApiResponse<WalletDTO> response = new ApiResponse<>("تم انساء المحفظة بنجاح", responseDto);
     return ResponseEntity.ok().body(response);
    }
    @GetMapping("/{walletId}/stats")
    public ResponseEntity<ApiResponse<WalletStatsDTO>> getWalletStats(@PathVariable("walletId") String walletId) {
        Wallet wallet = walletService.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("المحفظة غير موجودة"));
        Double totalIncome = transactionService.calculateTotalByWalletAndType(walletId, true);
        Double totalExpense = transactionService.calculateTotalByWalletAndType(walletId, false);
        Double balance = totalIncome - totalExpense;
        WalletStatsDTO statsDto = new WalletStatsDTO(totalIncome, totalExpense, balance);
        ApiResponse<WalletStatsDTO> response = new ApiResponse<>("تم جلب الاحصائيات بنجاح",statsDto);
        return ResponseEntity.ok(response);
    }
    // مسار جلب إحصائيات المصروفات مجمعة بالفئات للمخطط الدائري
    @GetMapping("/{walletId}/stats/categories")
    public ResponseEntity<ApiResponse<List<CategoryTotalProjection>>> getExpenseStatsByCategory(@PathVariable String walletId) {

        List<com.omar.expense_tracker_backend.repositories.CategoryTotalProjection> stats =
                transactionService.getExpenseStatsByCategory(walletId);

        ApiResponse<List<com.omar.expense_tracker_backend.repositories.CategoryTotalProjection>> response =
                new ApiResponse<>("تم جلب إحصائيات الفئات بنجاح", stats);

        return ResponseEntity.ok(response);
    }
}
