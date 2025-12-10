package com.academic.user.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JwtUtil
{
    static public final int expirationTime = 24 * 3600 * 1000;
    static String secretKey = "r-k.Uv4D@rrX2aYiLOJJC-)!XBD=-#[^i,&vykXQYtU6p.pF4'xQ#GZ-4AS+ri)vhBEAyQFfpZ+";
    static SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
//    MacAlgorithm alg = Jwts.SIG.HS512;
//    SecretKey key = alg.key().build();
    static Map<String, Object> inputClaims = new HashMap<>();
    //生成密钥
    public static String generateToken(String userId)
    {
        inputClaims.put("userId", userId);
        String token = Jwts.builder()
                .claims(inputClaims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .id(UUID.randomUUID().toString())
                .signWith(key)
                .compact();
        return token;
    }

    //解析密钥生成id
    public static String analyseToken(String token)
    {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId").toString();
    }
}
