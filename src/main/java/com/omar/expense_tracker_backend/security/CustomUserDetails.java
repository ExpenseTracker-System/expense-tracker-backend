package com.omar.expense_tracker_backend.security;

import com.omar.expense_tracker_backend.models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

// هذا الكلاس يطبق واجهة UserDetails ليكون "بطاقة الهوية" التي يفهمها Spring Security
public class CustomUserDetails implements UserDetails {

    private final User user; // كائن المستخدم الخاص بنا

    // الكونستركتر: نمرر له المستخدم الحقيقي، ليقوم بتغليفه
    public CustomUserDetails(User user) {
        this.user = user;
    }
    // هنا نخبر الحارس الأجنبي ما هي كلمة المرور
    @Override
    public String getPassword() {
        return user.getPassword();
    }
    // هنا نخبر الحارس أننا نستخدم الإيميل كاسم مستخدم
    @Override
    public String getUsername() {
        return user.getEmail();
    }
    // هذه الدالة لتحديد الصلاحيات (أدمن، مستخدم عادي). حالياً سنتركها فارغة (الكل مستخدم عادي)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return new ArrayList<>();
    }
    // بقية الدوال تخبر الحارس أن الحساب غير مغلق وغير محظور (نجعلها كلها ترجع true)
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    // دالة إضافية للحصول على المستخدم الأصلي إذا احتجناه
    public User getUser() {
        return user;
    }
}