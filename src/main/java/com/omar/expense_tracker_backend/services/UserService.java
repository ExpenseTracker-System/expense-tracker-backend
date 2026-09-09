package com.omar.expense_tracker_backend.services;

import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.repositories.UserRepository;
import com.omar.expense_tracker_backend.security.CustomUserDetails; // استيراد بطاقة الهوية
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder; // استيراد أداة التشفير
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("لا يوجد مستخدم بهذا البريد الإلكتروني: " + email));


        return new CustomUserDetails(user);
    }


    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(User user) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("البريد الالكتروني غير صحيح");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("كلمة المرور مطلوبة");
        }
        // إعطاء قيمة افتراضية للحقل الإجباري
        if (user.getAuthProvider() == null) {
            user.setAuthProvider("LOCAL");
        }
        String plainPassword = user.getPassword();
        String hashedPassword = passwordEncoder.encode(plainPassword);
        user.setPassword(hashedPassword);

        return userRepository.save(user);
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
}