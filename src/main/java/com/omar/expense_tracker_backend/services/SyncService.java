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

import java.nio.charset.StandardCharsets;
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

        LocalDateTime now = LocalDateTime.now();
        Map<String, String> walletIdMapping = new HashMap<>();

        // 1. حفظ وتحديث المحافظ القادمة من الهاتف مباشرة في PostgreSQL
        if (request.getWallets() != null) {
            for (WalletDTO wDto : request.getWallets()) {
                if (wDto.getId() == null || wDto.getId().isBlank()) continue;

                String targetWalletId = wDto.getId();
                Optional<Wallet> existingWalletOpt = walletRepository.findByIdIncludingDeleted(targetWalletId);

                // إذا كان المعرف مستخدماً لحساب آخر على نفس الهاتف، نولد معرفاً خاصاً بهذا المستخدم
                if (existingWalletOpt.isPresent() && !existingWalletOpt.get().getUser().getId().equals(userId)) {
                    targetWalletId = UUID.nameUUIDFromBytes((userId + ":wallet:" + wDto.getId()).getBytes(StandardCharsets.UTF_8)).toString();
                }

                walletIdMapping.put(wDto.getId(), targetWalletId);

                String name = (wDto.getName() != null && !wDto.getName().isBlank()) ? wDto.getName() : "المحفظة الشخصية";
                boolean isDeleted = wDto.getDeleted() != null && wDto.getDeleted();
                LocalDateTime createdAt = wDto.getCreatedAt() != null ? wDto.getCreatedAt() : now;

                walletRepository.upsertWallet(targetWalletId, name, userId, isDeleted, createdAt, now);
            }
        }

        // التأكد من وجود محفظة افتراضية واحدة على الأقل للمستخدم لربط أي معاملات يتيمة بها
        String fallbackWalletId = walletIdMapping.values().stream().findFirst().orElse(null);
        if (fallbackWalletId == null) {
            List<Wallet> userWallets = walletRepository.findByUserOrderByCreatedAtAsc(user);
            if (!userWallets.isEmpty()) {
                fallbackWalletId = userWallets.get(0).getId();
            } else {
                fallbackWalletId = UUID.nameUUIDFromBytes((userId + ":default_wallet").getBytes(StandardCharsets.UTF_8)).toString();
                walletRepository.upsertWallet(fallbackWalletId, "المحفظة الشخصية", userId, false, now, now);
            }
        }

        // 2. حفظ وتحديث المعاملات القادمة من الهاتف مباشرة في PostgreSQL
        if (request.getTransactions() != null) {
            for (TransactionDTO tDto : request.getTransactions()) {
                if (tDto.getId() == null || tDto.getId().isBlank()) continue;

                String targetTxId = tDto.getId();
                Optional<Transaction> existingTxOpt = transactionRepository.findByIdIncludingDeleted(targetTxId);

                if (existingTxOpt.isPresent() && !existingTxOpt.get().getWallet().getUser().getId().equals(userId)) {
                    targetTxId = UUID.nameUUIDFromBytes((userId + ":tx:" + tDto.getId()).getBytes(StandardCharsets.UTF_8)).toString();
                }

                // تحديد المحفظة الصحيحة التابعة للمستخدم
                String requestedWalletId = tDto.getWalletId();
                String resolvedWalletId = walletIdMapping.get(requestedWalletId);

                if (resolvedWalletId == null && requestedWalletId != null) {
                    Optional<Wallet> wOpt = walletRepository.findByIdIncludingDeleted(requestedWalletId);
                    if (wOpt.isPresent() && wOpt.get().getUser().getId().equals(userId)) {
                        resolvedWalletId = requestedWalletId;
                    }
                }
                if (resolvedWalletId == null) {
                    resolvedWalletId = fallbackWalletId;
                }

                Double amount = tDto.getAmount() != null ? tDto.getAmount() : 0.0;
                String category = (tDto.getCategory() != null && !tDto.getCategory().isBlank()) ? tDto.getCategory() : "أخرى";
                String note = tDto.getNote() != null ? tDto.getNote() : "";
                boolean isIncome = tDto.getIncome() != null && tDto.getIncome();
                LocalDateTime txDate = tDto.getTransactionDate() != null ? tDto.getTransactionDate() : now;
                boolean isDeleted = tDto.getDeleted() != null && tDto.getDeleted();
                LocalDateTime createdAt = tDto.getCreatedAt() != null ? tDto.getCreatedAt() : now;

                transactionRepository.upsertTransaction(
                        targetTxId, amount, category, note, isIncome, txDate, resolvedWalletId, isDeleted, createdAt, now
                );
            }
        }

        // 3. جلب كافة المحافظ والمعاملات المحدثة من السيرفر وإرجاعها للهاتف
        List<Wallet> serverWallets = walletRepository.findWalletsForSync(userId, clientLastSync);
        List<Transaction> serverTransactions = transactionRepository.findTransactionsForSync(userId, clientLastSync);

        List<WalletDTO> resWallets = serverWallets.stream().map(w -> {
            WalletDTO dto = new WalletDTO(w.getId(), w.getName(), userId);
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