package org.example.expert.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final long TOKEN_TIME = 60 * 60 * 1000L; // 60분

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret.key}") String secretKey) {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(Long userId, String email, String nickname, UserRole userRole) {
        Date date = new Date();

        return BEARER_PREFIX +
                Jwts.builder()
                        .subject(String.valueOf(userId))
                        .claim("email", email)
                        .claim("userRole", userRole.name())
                        .claim("nickname", nickname)
                        .expiration(new Date(date.getTime() + TOKEN_TIME))
                        .issuedAt(date) // 발급일
                        .signWith(key, Jwts.SIG.HS256) // 암호화 알고리즘
                        .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
