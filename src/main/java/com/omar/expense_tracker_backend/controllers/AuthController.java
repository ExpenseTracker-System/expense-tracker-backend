package com.omar.expense_tracker_backend.controllers;

import com.omar.expense_tracker_backend.dtos.AuthResponse;
import com.omar.expense_tracker_backend.dtos.LoginRequest;
import com.omar.expense_tracker_backend.dtos.UserDTO;
import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.responses.ApiResponse;
import com.omar.expense_tracker_backend.security.CustomUserDetails;
import com.omar.expense_tracker_backend.security.JwtUtil;
import com.omar.expense_tracker_backend.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@RequestBody LoginRequest loginRequest) {
        // 1. تسليم الإيميل والباسورد للمدير العام ليتأكد من صحتها
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );
        // 2. إذا مررنا من السطر السابق، يعني الباسورد صحيح. الآن نجلب المستخدم من قاعدة البيانات
        User user = userService.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("المستخدم غير موجود"));
        // 3. تحويل المستخدم إلى "بطاقة هوية" يفهمها مصنع التذاكر
        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        // 4. توليد تذكرة المرور (Token) السحرية
        String token = jwtUtil.generateToken(customUserDetails);
        // 5. تجهيز قوالب النقل (إخفاء الباسورد وإرسال البيانات المهمة فقط لفلاتر)
        UserDTO userDTO = new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAuthProvider()
        );
        AuthResponse authResponse = new AuthResponse(token, userDTO);
        // 6. تغليف الرد النهائي في ApiResponse وإرساله كـ (200 OK)
        ApiResponse<AuthResponse> response = new ApiResponse<>(true, "تم تسجيل الدخول بنجاح", authResponse);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> register(@RequestBody User user) {
        // 1. نرسل المستخدم لـ UserService لكي يحفظه (والذي سيقوم بدوره بتشفير الباسورد تلقائياً)
        User savedUser = userService.createUser(user);
        // 2. نجهز القالب النظيف (بدون باسورد) لإرجاعه
        UserDTO userDTO = new UserDTO(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getAuthProvider()
        );
        // 3. نغلفه بالرد الموحد
        ApiResponse<UserDTO> response = new ApiResponse<>(true, "تم إنشاء الحساب بنجاح", userDTO);

        return ResponseEntity.ok(response);
    }
}