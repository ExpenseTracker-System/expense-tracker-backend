package com.omar.expense_tracker_backend.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtUtil {

    // 1. المفتاح السري (الختم): يجب أن يكون طويلاً ومعقداً (256-bit).
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    // 2. مدة الصلاحية: 24 ساعة (محسوبة بالملي ثانية)
    private static final long JWT_EXPIRATION = 1000 * 60 * 60 * 24;

    //الدوال الأساسية

    // استخراج الإيميل (Username) من التوكن
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // توليد توكن جديد للمستخدم
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    // التحقق من أن التوكن صالح (يخص هذا المستخدم ولم ينتهِ)
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // دوال مساعدة (Helper Methods) للاستخدام الداخلي

    // دالة إنشاء التوكن الفعلي (تحدد وقت البداية، النهاية، والختم)
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // التأكد هل انتهى تاريخ التوكن؟
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // استخراج تاريخ الانتهاء
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // دالة عامة لاستخراج أي معلومة من الـ Payload
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);// كلمة apply تعني اننا نقول للمتغير خذ الكائن هذا ونفذ عليه التعليمات الموجوده في راس الداله
    }

    // هذه الدالة هي التي "تفك تشفير" التوكن للتأكد من صحته باستخدام المفتاح السري
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // تحويل النص السري إلى "مفتاح تشفير حقيقي" يفهمه النظام
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}