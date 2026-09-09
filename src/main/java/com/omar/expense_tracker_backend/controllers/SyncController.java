package com.omar.expense_tracker_backend.controllers;

import com.omar.expense_tracker_backend.dtos.SyncRequest;
import com.omar.expense_tracker_backend.dtos.SyncResponse;
import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.repositories.UserRepository;
import com.omar.expense_tracker_backend.services.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;
    private final UserRepository userRepository;

    public SyncController(SyncService syncService, UserRepository userRepository) {
        this.syncService = syncService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<SyncResponse> syncData(@RequestBody SyncRequest request, Authentication authentication) {

        // 1. استخراج البريد الإلكتروني من التوكن (Token)
        String email = authentication.getName();

        // 2. البحث عن المستخدم في قاعدة البيانات لجلب الـ ID الخاص به
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("المستخدم غير مصرح له"));

        // 3. تمرير الطلب إلى "العقل المدبر" ليقوم بالمزامنة
        SyncResponse response = syncService.synchronize(user.getId(), request);

        // 4. إرجاع الحقيبة المحدثة إلى فلاتر مع رمز النجاح 200
        return ResponseEntity.ok(response);
    }
}