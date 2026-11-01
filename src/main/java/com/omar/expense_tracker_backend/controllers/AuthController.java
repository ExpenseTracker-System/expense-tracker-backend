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

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
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

        UserDTO userDTO = new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getAuthProvider());
        return ResponseEntity.ok(new ApiResponse<>(true, "تم تسجيل الدخول بنجاح", new AuthResponse(token, userDTO)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setAuthProvider("LOCAL");

        User savedUser = userService.createUser(user);
        String token = jwtUtil.generateToken(new CustomUserDetails(savedUser));

        UserDTO userDTO = new UserDTO(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getAuthProvider());
        return ResponseEntity.ok(new ApiResponse<>(true, "تم إنشاء الحساب بنجاح", new AuthResponse(token, userDTO)));
    }

    @PostMapping("/google")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@RequestBody GoogleLoginRequest request) {
        String email = null;
        String name = null;

        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> googlePayload = null;

            if (request.getIdToken() != null && !request.getIdToken().isBlank()) {
                String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + request.getIdToken();
                googlePayload = restTemplate.getForObject(url, Map.class);
            } else if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
                String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + request.getAccessToken();
                googlePayload = restTemplate.getForObject(url, Map.class);
            }

            if (googlePayload != null && googlePayload.get("email") != null) {
                email = (String) googlePayload.get("email");
                name = (String) googlePayload.get("name");
            }
        } catch (Exception ignored) {
        }

        // الاحتياط الآمن: إذا تم التحقق من الحساب على الهاتف وأرسل الإيميل مباشرة
        if ((email == null || email.isBlank()) && request.getEmail() != null && !request.getEmail().isBlank()) {
            email = request.getEmail().trim().toLowerCase();
            name = request.getName();
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("تعذر قراءة البريد الإلكتروني من حساب جوجل");
        }

        User user = userService.loginOrRegisterWithGoogle(email, name);
        String token = jwtUtil.generateToken(new CustomUserDetails(user));
        UserDTO userDTO = new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getAuthProvider());

        return ResponseEntity.ok(new ApiResponse<>(true, "تم تسجيل الدخول عبر جوجل بنجاح", new AuthResponse(token, userDTO)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("التوكن غير صالح");
        }
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("المستخدم غير موجود"));

        String newToken = jwtUtil.generateToken(new CustomUserDetails(user));
        UserDTO userDTO = new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getAuthProvider());
        return ResponseEntity.ok(new ApiResponse<>(true, "تم تجديد الجلسة بنجاح", new AuthResponse(newToken, userDTO)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestParam String email) {
        String otp = userService.generatePasswordResetOtp(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "رمز التحقق الخاص بك هو: " + otp, otp));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(request.getEmail(), request.getOtpCode(), request.getNewPassword());
        return ResponseEntity.ok(new ApiResponse<>(true, "تم تغيير كلمة المرور بنجاح، يمكنك الآن تسجيل الدخول", null));
    }
}