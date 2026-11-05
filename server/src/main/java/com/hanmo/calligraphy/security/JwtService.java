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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * JWT 签发与解析服务。
 *
 * <p>使用 HMAC-SHA 对令牌签名（具体算法由密钥长度决定），密钥来自 app.jwt.secret；
 * 有效期由 app.jwt.expire-hours 控制，默认 8 小时。令牌内携带用户 ID、姓名、账号类型与角色编码，
 * 供 JwtAuthenticationFilter 恢复登录态。</p>
 */
@Service
public class JwtService {

    // HMAC 签名密钥（由 app.jwt.secret 派生）
    private final SecretKey key;
    // 令牌有效期（毫秒），由 app.jwt.expire-hours 换算
    private final long expireMillis;

    /**
     * @param secret      签名密钥（app.jwt.secret）
     * @param expireHours 有效期小时数（app.jwt.expire-hours，默认 8）
     */
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expire-hours:8}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 60L * 60L * 1000L;
    }

    /**
     * 为登录成功的账号签发 JWT。
     *
     * <p>副作用：无（纯计算，不写库）。</p>
     *
     * @param user 账号
     * @param role 角色，可为空（为空时 role / roleName 声明为 null）
     * @return 签名后的 JWT 字符串
     */
    public String generate(SysUser user, SysRole role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMillis);
        return Jwts.builder()
                // 标准 subject 与自定义 uid 都放用户 ID，便于不同解析方式取用
                .subject(String.valueOf(user.getId()))
                .claim("uid", user.getId())
                .claim("name", user.getName())
                .claim("userType", user.getUserType())
                .claim("role", role == null ? null : role.getCode())
                .claim("roleName", role == null ? null : role.getName())
                // 密码指纹：改密后指纹变化，旧令牌在过滤器里会被判定失效
                .claim("pwd", passwordFingerprint(user))
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /**
     * 计算密码指纹，写入令牌的 {@code pwd} 声明。
     *
     * <p>取 BCrypt 哈希的 SHA-256 前 8 字节转十六进制。改密后哈希变化 → 指纹变化，
     * 过滤器比对不上就把该令牌当作失效，等于「改密后旧令牌立即作废」，无需额外的版本号字段。</p>
     *
     * <p>微信登录的教师/家长没有密码，统一返回 "-"，不参与失效判断。</p>
     *
     * @param user 账号
     * @return 指纹字符串
     */
    public static String passwordFingerprint(SysUser user) {
        if (user == null || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            return "-";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(user.getPasswordHash().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 在所有 JVM 上都必须存在；退化成固定值只是让令牌更容易失效，不会放行
            return "-";
        }
    }

    /**
     * 解析并校验令牌。
     *
     * <p>校验签名与 exp 过期时间；失败时抛出 io.jsonwebtoken.JwtException 的子类
     * （如 ExpiredJwtException、SignatureException），由调用方决定如何处理。</p>
     *
     * @param token 不含 "Bearer " 前缀的 JWT 字符串
     * @return 令牌中的 claims
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
