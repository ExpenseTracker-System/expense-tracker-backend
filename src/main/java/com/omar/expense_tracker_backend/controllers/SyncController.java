package com.omar.expense_tracker_backend.controllers;

import com.omar.expense_tracker_backend.dtos.SyncRequest;
import com.omar.expense_tracker_backend.dtos.SyncResponse;
import com.omar.expense_tracker_backend.exceptions.ResourceNotFoundException;
import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.repositories.UserRepository;
import com.omar.expense_tracker_backend.security.CustomUserDetails;
import com.omar.expense_tracker_backend.security.JwtUtil;
import com.omar.expense_tracker_backend.services.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public SyncController(SyncService syncService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.syncService = syncService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<SyncResponse> syncData(@RequestBody SyncRequest request, Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير مصرح له"));

        SyncResponse response = syncService.synchronize(user.getId(), request);

        // توليد توكن جديد بصلاحية 30 يوماً جديدة تلقائياً مع كل مزامنة (Sliding Session)
        String refreshedToken = jwtUtil.generateToken(new CustomUserDetails(user));
        response.setNewToken(refreshedToken);

        return ResponseEntity.ok(response);
    }
}