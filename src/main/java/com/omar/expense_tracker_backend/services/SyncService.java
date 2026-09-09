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

    // لافتة الأمان: المزامنة إما تنجح بالكامل أو تفشل بالكامل ولا تشوه البيانات
    @Transactional
    public SyncResponse synchronize(String userId, SyncRequest request) {
        // 1. التأكد من وجود المستخدم
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("المستخدم غير موجود"));

        // 2. تحديد نقطة البداية الزمنية
        LocalDateTime clientLastSync = request.getLastSyncTime();
        if (clientLastSync == null) {
            // إذا كان الهاتف جديداً، نعتبر أن آخر مزامنة كانت في الماضي السحيق لنجلب له كل شيء
            clientLastSync = LocalDateTime.of(2000, 1, 1, 0, 0);
        }

        // القسم الأول: الرفع (Upstream) - من الهاتف إلى السيرفر

        // أ. معالجة المحافظ القادمة من الهاتف
        if (request.getWallets() != null) {
            for (WalletDTO wDto : request.getWallets()) {
                Optional<Wallet> existingWallet = walletRepository.findById(wDto.getId());

                if (existingWallet.isPresent()) {
                    // خوارزمية (الأحدث يفوز - Last Write Wins)
                    if (wDto.getUpdatedAt().isAfter(existingWallet.get().getUpdatedAt())) {
                        Wallet w = existingWallet.get();
                        w.setName(wDto.getName());
                        w.setDeleted(wDto.getDeleted());
                        walletRepository.save(w);
                    }
                } else {
                    // إذا لم تكن موجودة، فهذه محفظة جديدة تم إنشاؤها بدون إنترنت
                    Wallet w = new Wallet();
                    w.setId(wDto.getId());
                    w.setName(wDto.getName());
                    w.setUser(user);
                    w.setDeleted(wDto.getDeleted() != null ? wDto.getDeleted() : false);
                    walletRepository.save(w);
                }
            }
        }

        // ب. معالجة المعاملات القادمة من الهاتف
        if (request.getTransactions() != null) {
            for (TransactionDTO tDto : request.getTransactions()) {
                Optional<Transaction> existingTx = transactionRepository.findById(tDto.getId());

                if (existingTx.isPresent()) {
                    // خوارزمية (الأحدث يفوز)
                    if (tDto.getUpdatedAt().isAfter(existingTx.get().getUpdatedAt())) {
                        Transaction t = existingTx.get();
                        t.setAmount(tDto.getAmount());
                        t.setCategory(tDto.getCategory());
                        t.setNote(tDto.getNote());
                        t.setIncome(tDto.getIncome());
                        t.setTransactionDate(tDto.getTransactionsDate());
                        t.setDeleted(tDto.getDeleted());
                        transactionRepository.save(t);
                    }
                } else {
                    // معاملة جديدة
                    Optional<Wallet> wallet = walletRepository.findById(tDto.getWalletId());
                    if (wallet.isPresent()) {
                        Transaction t = new Transaction();
                        t.setId(tDto.getId());
                        t.setAmount(tDto.getAmount());
                        t.setCategory(tDto.getCategory());
                        t.setNote(tDto.getNote());
                        t.setIncome(tDto.getIncome());
                        t.setTransactionDate(tDto.getTransactionsDate());
                        t.setWallet(wallet.get());
                        t.setDeleted(tDto.getDeleted() != null ? tDto.getDeleted() : false);
                        transactionRepository.save(t);
                    }
                }
            }
        }

        // القسم الثاني: التحميل (Downstream) - من السيرفر إلى الهاتف

        // استخدام دوال (Native Queries) التي تخترق الحذف الناعم لجلب التعديلات الجديدة
        List<Wallet> serverWallets = walletRepository.findWalletsForSync(userId, clientLastSync);
        List<Transaction> serverTransactions = transactionRepository.findTransactionsForSync(userId, clientLastSync);

        // تحويل كائنات السيرفر إلى قوالب DTO لفلاتر
        List<WalletDTO> resWallets = serverWallets.stream().map(w -> {
            WalletDTO dto = new WalletDTO(w.getId(), w.getName(), w.getUser().getId());
            dto.setDeleted(w.getDeleted());
            dto.setUpdatedAt(w.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());

        List<TransactionDTO> resTransactions = serverTransactions.stream().map(t -> {
            TransactionDTO dto = new TransactionDTO(t.getId(), t.getAmount(), t.getCategory(), t.getNote(), t.getIncome(), t.getTransactionDate(), t.getWallet().getId());
            dto.setDeleted(t.getDeleted());
            dto.setUpdatedAt(t.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());

        // القسم الثالث: إغلاق الحقيبة وإرسالها

        SyncResponse response = new SyncResponse();
        response.setSyncTime(LocalDateTime.now()); // وقت السيرفر الحالي
        response.setWallets(resWallets);
        response.setTransactions(resTransactions);

        return response;
    }
}