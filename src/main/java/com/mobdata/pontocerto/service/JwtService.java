package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.model.Perfil;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String gerarToken(String usuario, UUID empresaId, Perfil perfil) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        var builder = Jwts.builder()
                .subject(usuario)
                .claim("perfil", perfil.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key);

        if (empresaId != null) {
            builder.claim("empresaId", empresaId.toString());
        }

        return builder.compact();
    }

    public String extrairUsuario(String token) {
        return getClaims(token).getSubject();
    }

    public UUID extrairEmpresaId(String token) {
        String empresaIdStr = getClaims(token).get("empresaId", String.class);
        return empresaIdStr != null ? UUID.fromString(empresaIdStr) : null;
    }

    public Perfil extrairPerfil(String token) {
        String perfilStr = getClaims(token).get("perfil", String.class);
        return Perfil.valueOf(perfilStr);
    }

    public boolean tokenValido(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
