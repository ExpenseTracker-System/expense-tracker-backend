package com.omar.expense_tracker_backend.services;

import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.repositories.UserRepository;
import com.omar.expense_tracker_backend.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:}")
    private String senderEmail;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("لا يوجد مستخدم بهذا البريد الإلكتروني: " + email));
        return new CustomUserDetails(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    @Transactional
    public User createUser(User user) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("البريد الإلكتروني غير صحيح");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("كلمة المرور مطلوبة");
        }

        String normalizedEmail = user.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("البريد الإلكتروني مسجل بالفعل");
        }

        user.setEmail(normalizedEmail);
        if (user.getAuthProvider() == null || user.getAuthProvider().isBlank()) {
            user.setAuthProvider("LOCAL");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public User loginOrRegisterWithGoogle(String email, String name) {
        String normalizedEmail = email.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(normalizedEmail);
            newUser.setUsername(name != null && !name.isBlank() ? name : normalizedEmail.split("@")[0]);
            newUser.setAuthProvider("GOOGLE");
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            return userRepository.save(newUser);
        });
    }

    @Transactional
    public String generatePasswordResetOtp(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("لا يوجد حساب مرتبط بهذا البريد الإلكتروني"));

        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000));
        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        log.info("🔑 [Password Reset OTP] For {}: {}", user.getEmail(), otp);

        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            try {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("api-key", brevoApiKey);
                headers.set("Content-Type", "application/json");

                String htmlContent = "<div dir='rtl' style='font-family: Arial, sans-serif; padding: 20px;'>"
                        + "<h2>تطبيق محفظتي الذكية</h2>"
                        + "<p>مرحباً <b>" + user.getUsername() + "</b>،</p>"
                        + "<p>رمز التحقق الخاص بك لاستعادة كلمة المرور هو:</p>"
                        + "<h1 style='color: #1565C0; letter-spacing: 4px;'>" + otp + "</h1>"
                        + "</div>";

                Map<String, Object> body = Map.of(
                        "sender", Map.of("name", "محفظتي الذكية", "email", senderEmail),
                        "to", List.of(Map.of("email", user.getEmail())),
                        "subject", "رمز استعادة كلمة المرور - محفظتي الذكية",
                        "htmlContent", htmlContent
                );

                org.springframework.http.HttpEntity> entity =
                        new org.springframework.http.HttpEntity<>(body, headers);
                restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", entity, String.class);
            } catch (Exception e) {
                // لا نوقف العملية إذا فشل الإيميل، بل نرجع الرمز للتطبيق مباشرة
                log.warn("Brevo email skipped, returning OTP directly to app: {}", e.getMessage());
            }
        }
        return otp;
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("البريد الإلكتروني غير صحيح"));

        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp.trim())) {
            throw new IllegalArgumentException("رمز التحقق غير صحيح");
        }
        if (user.getResetOtpExpiry() == null || user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("انتهت صلاحية رمز التحقق، يرجى طلب رمز جديد");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
}