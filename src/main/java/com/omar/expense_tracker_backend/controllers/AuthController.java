package com.omar.expense_tracker_backend.controllers;

import com.omar.expense_tracker_backend.dtos.*;
import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.responses.ApiResponse;
import com.omar.expense_tracker_backend.security.CustomUserDetails;
import com.omar.expense_tracker_backend.security.JwtUtil;
import com.omar.expense_tracker_backend.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@Valid @RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getEmail().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
        );

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("المستخدم غير موجود"));

        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(customUserDetails);

        UserDTO userDTO = new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAuthProvider()
        );
        AuthResponse authResponse = new AuthResponse(token, userDTO);

        return ResponseEntity.ok(new ApiResponse<>(true, "تم تسجيل الدخول بنجاح", authResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setAuthProvider("LOCAL");

        User savedUser = userService.createUser(user);
        CustomUserDetails customUserDetails = new CustomUserDetails(savedUser);
        String token = jwtUtil.generateToken(customUserDetails);

        UserDTO userDTO = new UserDTO(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getAuthProvider()
        );
        AuthResponse authResponse = new AuthResponse(token, userDTO);

        return ResponseEntity.ok(new ApiResponse<>(true, "تم إنشاء الحساب بنجاح", authResponse));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + request.getIdToken();
            @SuppressWarnings("unchecked")
            Map<String, Object> googlePayload = restTemplate.getForObject(url, Map.class);

            if (googlePayload == null || googlePayload.get("email") == null) {
                throw new IllegalArgumentException("رمز جوجل غير صالح");
            }

            String email = (String) googlePayload.get("email");
            String name = (String) googlePayload.get("name");

            User user = userService.loginOrRegisterWithGoogle(email, name);
            String token = jwtUtil.generateToken(new CustomUserDetails(user));
            UserDTO userDTO = new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getAuthProvider());

            return ResponseEntity.ok(new ApiResponse<>(true, "تم تسجيل الدخول عبر جوجل بنجاح", new AuthResponse(token, userDTO)));
        } catch (Exception e) {
            throw new IllegalArgumentException("فشل التحقق من حساب جوجل، تأكد من صلاحية الرمز");
        }
    }

    // مسار التجديد الانزلاقي للتوكن (Sliding Session Refresh)
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("التوكن غير صالح أو منتهي الصلاحية");
        }

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("المستخدم غير موجود"));

        String newToken = jwtUtil.generateToken(new CustomUserDetails(user));
        UserDTO userDTO = new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getAuthProvider());
        AuthResponse authResponse = new AuthResponse(newToken, userDTO);

        return ResponseEntity.ok(new ApiResponse<>(true, "تم تجديد الجلسة بنجاح", authResponse));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(@RequestParam String email) {
        userService.generatePasswordResetOtp(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "تم إرسال رمز التحقق لاستعادة كلمة المرور", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(request.getEmail(), request.getOtpCode(), request.getNewPassword());
        return ResponseEntity.ok(new ApiResponse<>(true, "تم تغيير كلمة المرور بنجاح، يمكنك الآن تسجيل الدخول", null));
    }
}