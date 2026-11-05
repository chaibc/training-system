package com.hanmo.calligraphy.security;

import com.hanmo.calligraphy.system.entity.SysRole;
import com.hanmo.calligraphy.system.entity.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expireMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expire-hours:8}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 60L * 60L * 1000L;
    }

    public String generate(SysUser user, SysRole role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMillis);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("uid", user.getId())
                .claim("name", user.getName())
                .claim("userType", user.getUserType())
                .claim("role", role == null ? null : role.getCode())
                .claim("roleName", role == null ? null : role.getName())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
