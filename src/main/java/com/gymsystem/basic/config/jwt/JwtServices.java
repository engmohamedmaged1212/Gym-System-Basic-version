package com.gymsystem.basic.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtServices {
    private final JwtConfig jwtConfig;

    /** Short-lived token sent in the Authorization header. Carries role + permissions. */
    public Jwt generateAccessTokens(TokenSubject subject) {
        var claims = Jwts.claims()
                .subject(subject.staffId().toString())
                .add("type", "access")
                .add("email", subject.email())
                .add("role", subject.role())
                .add("branch_id", subject.branchId())
                .add("sid", subject.sessionId())
                .add("permissions", subject.permissions())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * jwtConfig.getAccessTokenExpiration()))
                .build();
        return sign(claims);
    }

    /** Long-lived token kept in an HttpOnly cookie. Carries only who and which session. */
    public Jwt generateRefreshTokens(TokenSubject subject) {
        var claims = Jwts.claims()
                .subject(subject.staffId().toString())
                .add("type", "refresh")
                .add("sid", subject.sessionId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * jwtConfig.getRefreshTokenExpiration()))
                .build();
        return sign(claims);
    }

    private Jwt sign(Claims claims) {
        String tokenValue = Jwts.builder()
                .claims(claims)
                .signWith(jwtConfig.getSecretKey())
                .compact();
        return new Jwt(claims, tokenValue);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtConfig.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Returns null when the token is malformed, tampered with, or expired. */
    public Jwt parseToken(String token) {
        try {
            return new Jwt(getClaims(token), token);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
