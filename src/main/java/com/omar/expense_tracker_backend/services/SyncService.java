package com.omar.expense_tracker_backend.services;

import com.omar.expense_tracker_backend.dtos.SyncRequest;
import com.omar.expense_tracker_backend.dtos.SyncResponse;
import com.omar.expense_tracker_backend.dtos.TransactionDTO;
import com.omar.expense_tracker_backend.dtos.WalletDTO;
import com.omar.expense_tracker_backend.models.Transaction;
import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.models.Wallet;
import com.omar.expense_tracker_backend.repositories.TransactionRepository;
import com.omar.expense_tracker_backend.repositories.UserRepository;
import com.omar.expense_tracker_backend.repositories.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SyncService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public SyncService(WalletRepository walletRepository, TransactionRepository transactionRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SyncResponse synchronize(String userId, SyncRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("المستخدم غير موجود"));

        LocalDateTime clientLastSync = request.getLastSyncTime();
        if (clientLastSync == null) {
            clientLastSync = LocalDateTime.of(2000, 1, 1, 0, 0);
        }

        // خريطة لتتبع المحافظ في حال تغير معرفها عند تبديل الحسابات على نفس الهاتف
        Map<String, Wallet> userWalletsCache = new HashMap<>();

        // 1. معالجة المحافظ القادمة من الهاتف (Upstream Wallets)
        if (request.getWallets() != null) {
            for (WalletDTO wDto : request.getWallets()) {
                if (wDto.getId() == null || wDto.getId().isBlank()) continue;

                LocalDateTime incomingUpdatedAt = wDto.getUpdatedAt() != null ? wDto.getUpdatedAt() : LocalDateTime.now();
                Optional<Wallet> existingWalletOpt = walletRepository.findByIdIncludingDeleted(wDto.getId());

                if (existingWalletOpt.isPresent()) {
                    Wallet w = existingWalletOpt.get();
                    if (w.getUser().getId().equals(userId)) {
                        if (w.getUpdatedAt() == null || incomingUpdatedAt.isAfter(w.getUpdatedAt())) {
                            w.setName(wDto.getName() != null ? wDto.getName() : "محفظة");
                            w.setDeleted(wDto.getDeleted() != null ? wDto.getDeleted() : false);
                            w.setUpdatedAt(LocalDateTime.now());
                            w = walletRepository.save(w);
                        }
                        userWalletsCache.put(wDto.getId(), w);
                    }
                } else {
                    Wallet w = new Wallet();
                    w.setId(wDto.getId());
                    w.setName(wDto.getName() != null ? wDto.getName() : "المحفظة الشخصية");
                    w.setUser(user);
                    w.setDeleted(wDto.getDeleted() != null ? wDto.getDeleted() : false);
                    w.setCreatedAt(wDto.getCreatedAt() != null ? wDto.getCreatedAt() : LocalDateTime.now());
                    w.setUpdatedAt(LocalDateTime.now());
                    w = walletRepository.save(w);
                    userWalletsCache.put(wDto.getId(), w);
                }
            }
            // إجبار Hibernate على حفظ المحافظ في PostgreSQL فوراً قبل إدراج المعاملات التابعة لها
            walletRepository.flush();
        }

        // 2. معالجة المعاملات القادمة من الهاتف (Upstream Transactions)
        if (request.getTransactions() != null) {
            for (TransactionDTO tDto : request.getTransactions()) {
                if (tDto.getId() == null || tDto.getId().isBlank()) continue;

                LocalDateTime incomingUpdatedAt = tDto.getUpdatedAt() != null ? tDto.getUpdatedAt() : LocalDateTime.now();
                LocalDateTime txDate = tDto.getTransactionDate() != null ? tDto.getTransactionDate() : LocalDateTime.now();
                Boolean isIncome = tDto.getIncome() != null ? tDto.getIncome() : false;
                Double amount = tDto.getAmount() != null ? tDto.getAmount() : 0.0;
                String category = (tDto.getCategory() != null && !tDto.getCategory().isBlank()) ? tDto.getCategory() : "أخرى";

                Optional<Transaction> existingTxOpt = transactionRepository.findByIdIncludingDeleted(tDto.getId());

                if (existingTxOpt.isPresent()) {
                    Transaction t = existingTxOpt.get();
                    if (t.getWallet().getUser().getId().equals(userId)) {
                        if (t.getUpdatedAt() == null || incomingUpdatedAt.isAfter(t.getUpdatedAt())) {
                            t.setAmount(amount);
                            t.setCategory(category);
                            t.setNote(tDto.getNote());
                            t.setIncome(isIncome);
                            t.setTransactionDate(txDate);
                            t.setDeleted(tDto.getDeleted() != null ? tDto.getDeleted() : false);
                            t.setUpdatedAt(LocalDateTime.now());
                            transactionRepository.save(t);
                        }
                    }
                } else {
                    Wallet targetWallet = userWalletsCache.get(tDto.getWalletId());
                    if (targetWallet == null && tDto.getWalletId() != null) {
                        Optional<Wallet> walletOpt = walletRepository.findByIdIncludingDeleted(tDto.getWalletId());
                        if (walletOpt.isPresent() && walletOpt.get().getUser().getId().equals(userId)) {
                            targetWallet = walletOpt.get();
                        }
                    }
                    // إذا لم تكن المحفظة موجودة لهذا المستخدم، نربطها بأول محفظة للمستخدم أو ننشئ محفظة افتراضية
                    if (targetWallet == null) {
                        List<Wallet> userWallets = walletRepository.findByUserOrderByCreatedAtAsc(user);
                        if (!userWallets.isEmpty()) {
                            targetWallet = userWallets.get(0);
                        } else {
                            Wallet defaultWallet = new Wallet();
                            defaultWallet.setId(UUID.randomUUID().toString());
                            defaultWallet.setName("المحفظة الشخصية");
                            defaultWallet.setUser(user);
                            defaultWallet.setCreatedAt(LocalDateTime.now());
                            defaultWallet.setUpdatedAt(LocalDateTime.now());
                            targetWallet = walletRepository.saveAndFlush(defaultWallet);
                        }
                    }

                    Transaction t = new Transaction();
                    t.setId(tDto.getId());
                    t.setAmount(amount);
                    t.setCategory(category);
                    t.setNote(tDto.getNote());
                    t.setIncome(isIncome);
                    t.setTransactionDate(txDate);
                    t.setWallet(targetWallet);
                    t.setDeleted(tDto.getDeleted() != null ? tDto.getDeleted() : false);
                    t.setCreatedAt(tDto.getCreatedAt() != null ? tDto.getCreatedAt() : LocalDateTime.now());
                    t.setUpdatedAt(LocalDateTime.now());
                    transactionRepository.save(t);
                }
            }
            transactionRepository.flush();
        }

        // 3. جلب أحدث البيانات من السيرفر وإرجاعها للهاتف (Downstream)
        List<Wallet> serverWallets = walletRepository.findWalletsForSync(userId, clientLastSync);
        List<Transaction> serverTransactions = transactionRepository.findTransactionsForSync(userId, clientLastSync);

        List<WalletDTO> resWallets = serverWallets.stream().map(w -> {
            WalletDTO dto = new WalletDTO(w.getId(), w.getName(), w.getUser().getId());
            dto.setDeleted(w.getDeleted());
            dto.setCreatedAt(w.getCreatedAt());
            dto.setUpdatedAt(w.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());

        List<TransactionDTO> resTransactions = serverTransactions.stream().map(t -> {
            TransactionDTO dto = new TransactionDTO(
                    t.getId(), t.getAmount(), t.getCategory(), t.getNote(),
                    t.getIncome(), t.getTransactionDate(), t.getWallet().getId()
            );
            dto.setDeleted(t.getDeleted());
            dto.setCreatedAt(t.getCreatedAt());
            dto.setUpdatedAt(t.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());

        SyncResponse response = new SyncResponse();
        response.setSyncTime(LocalDateTime.now());
        response.setWallets(resWallets);
        response.setTransactions(resTransactions);

        return response;
    }
}