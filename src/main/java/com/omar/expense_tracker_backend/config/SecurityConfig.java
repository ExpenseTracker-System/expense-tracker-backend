package com.omar.expense_tracker_backend.config;

import com.omar.expense_tracker_backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, AuthenticationProvider authenticationProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. إيقاف حماية CSRF (لأننا نستخدم التوكن ولا نستخدم المتصفحات)
                .csrf(csrf -> csrf.disable())
                // 2. قوانين المرور والمسارات
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // مسار التسجيل والدخول مفتوح للجميع
                        .anyRequest().authenticated()                // أي مسار آخر (محافظ، معاملات) مغلق ويحتاج توكن
                )
                // 3. إدارة الجلسات (نجعلها Stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. دمج موظف المطابقة الذي صنعناه
                .authenticationProvider(authenticationProvider)

                // 5. وضع حارسنا (الـ Filter) أمام البوابة الرئيسية
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}