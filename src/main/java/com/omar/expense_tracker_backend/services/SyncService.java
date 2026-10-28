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
import java.util.List;
import java.util.Optional;
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

        // 1. جلب التعديلات الموجودة في السيرفر قبل رفع بيانات الهاتف لمنع إعادة إرسال ما رفعه الهاتف للتو
        List<Wallet> serverWallets = walletRepository.findWalletsForSync(userId, clientLastSync);
        List<Transaction> serverTransactions = transactionRepository.findTransactionsForSync(userId, clientLastSync);

        // 2. القسم الأول: الرفع (Upstream) - معالجة المحافظ القادمة من الهاتف
        if (request.getWallets() != null) {
            for (WalletDTO wDto : request.getWallets()) {
                if (wDto.getId() == null || wDto.getId().isBlank()) continue;

                LocalDateTime incomingUpdatedAt = wDto.getUpdatedAt() != null ? wDto.getUpdatedAt() : LocalDateTime.now();
                Optional<Wallet> existingWalletOpt = walletRepository.findByIdIncludingDeleted(wDto.getId());

                if (existingWalletOpt.isPresent()) {
                    Wallet w = existingWalletOpt.get();
                    if (!w.getUser().getId().equals(userId)) {
                        continue;
                    }
                    if (w.getUpdatedAt() == null || incomingUpdatedAt.isAfter(w.getUpdatedAt())) {
                        w.setName(wDto.getName());
                        w.setDeleted(wDto.getDeleted() != null ? wDto.getDeleted() : false);
                        w.setUpdatedAt(LocalDateTime.now());
                        walletRepository.save(w);
                    }
                } else {
                    Wallet w = new Wallet();
                    w.setId(wDto.getId());
                    w.setName(wDto.getName());
                    w.setUser(user);
                    w.setDeleted(wDto.getDeleted() != null ? wDto.getDeleted() : false);
                    w.setCreatedAt(wDto.getCreatedAt() != null ? wDto.getCreatedAt() : LocalDateTime.now());
                    w.setUpdatedAt(LocalDateTime.now());
                    walletRepository.save(w);
                }
            }
        }

        // 3. معالجة المعاملات القادمة من الهاتف
        if (request.getTransactions() != null) {
            for (TransactionDTO tDto : request.getTransactions()) {
                if (tDto.getId() == null || tDto.getId().isBlank()) continue;

                LocalDateTime incomingUpdatedAt = tDto.getUpdatedAt() != null ? tDto.getUpdatedAt() : LocalDateTime.now();
                Optional<Transaction> existingTxOpt = transactionRepository.findByIdIncludingDeleted(tDto.getId());

                if (existingTxOpt.isPresent()) {
                    Transaction t = existingTxOpt.get();
                    if (!t.getWallet().getUser().getId().equals(userId)) {
                        continue;
                    }
                    if (t.getUpdatedAt() == null || incomingUpdatedAt.isAfter(t.getUpdatedAt())) {
                        t.setAmount(tDto.getAmount());
                        t.setCategory(tDto.getCategory());
                        t.setNote(tDto.getNote());
                        t.setIncome(tDto.getIncome());
                        t.setTransactionDate(tDto.getTransactionDate());
                        t.setDeleted(tDto.getDeleted() != null ? tDto.getDeleted() : false);
                        t.setUpdatedAt(LocalDateTime.now());
                        transactionRepository.save(t);
                    }
                } else {
                    Optional<Wallet> walletOpt = walletRepository.findByIdIncludingDeleted(tDto.getWalletId());
                    if (walletOpt.isPresent() && walletOpt.get().getUser().getId().equals(userId)) {
                        Transaction t = new Transaction();
                        t.setId(tDto.getId());
                        t.setAmount(tDto.getAmount());
                        t.setCategory(tDto.getCategory());
                        t.setNote(tDto.getNote());
                        t.setIncome(tDto.getIncome());
                        t.setTransactionDate(tDto.getTransactionDate());
                        t.setWallet(walletOpt.get());
                        t.setDeleted(tDto.getDeleted() != null ? tDto.getDeleted() : false);
                        t.setCreatedAt(tDto.getCreatedAt() != null ? tDto.getCreatedAt() : LocalDateTime.now());
                        t.setUpdatedAt(LocalDateTime.now());
                        transactionRepository.save(t);
                    }
                }
            }
        }

        // 4. القسم الثاني: التحميل (Downstream) - تحويل التعديلات لإرسالها إلى الهاتف
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