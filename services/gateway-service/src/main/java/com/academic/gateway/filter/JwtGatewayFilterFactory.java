package com.academic.gateway.filter;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    public JwtGatewayFilterFactory() { super(Object.class); }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (auth == null || !auth.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            String token = auth.substring(7).trim();
            if (jwtSecret == null || jwtSecret.isBlank()) {
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return exchange.getResponse().setComplete();
            }
            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Jws<Claims> jws = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token);
                Claims body = jws.getPayload();
                String userId = body.getSubject();
                if ((userId == null || userId.isBlank()) && body.get("userId") != null) {
                    userId = body.get("userId").toString();
                }
                if (userId == null || userId.isBlank()) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                String role = null;
                Object roleObj = body.get("role");
                if (roleObj == null) roleObj = body.get("roles");
                if (roleObj != null) role = roleObj.toString();

                ServerHttpRequest mutated = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role == null ? "" : role)
                        .build();

                return chain.filter(exchange.mutate().request(mutated).build());
            } catch (JwtException ex) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }
}
