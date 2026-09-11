package com.mobdata.pontocerto.config;


import com.mobdata.pontocerto.security.TenantContext;
import com.mobdata.pontocerto.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtService.tokenValido(token)) {
            String usuario = jwtService.extrairUsuario(token);
            UUID usuarioId = jwtService.extrairUsuarioId(token);
            var empresaId = jwtService.extrairEmpresaId(token);
            var perfil = jwtService.extrairPerfil(token);

            TenantContext.set(empresaId, perfil, usuarioId);
            var authorities = List.of(new SimpleGrantedAuthority(perfil.name()));
            var authentication = new UsernamePasswordAuthenticationToken(usuario, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear(); // limpa sempre, mesmo se der erro — evita "vazar" dados entre requisições
        }
    }
}
