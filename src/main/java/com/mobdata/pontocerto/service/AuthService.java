package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.LoginRequestDTO;
import com.mobdata.pontocerto.dto.LoginResponseDTO;
import com.mobdata.pontocerto.exception.CredenciaisInvalidasException;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AlocacaoRepository alocacaoRepository;

    public LoginResponseDTO login(LoginRequestDTO request) {
        Usuario usuario = usuarioRepository.findByUsuario(request.usuario())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário ou senha inválidos"));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Usuário ou senha inválidos");
        }

        // Empresa inativa não deixa ninguém dela entrar (exceto SUPERADMIN, que não tem empresa)
        if (usuario.getEmpresa() != null && !usuario.getEmpresa().isAtiva()) {
            throw new CredenciaisInvalidasException("Usuário ou senha inválidos");
        }

        String token = jwtService.gerarToken(
                usuario.getId(),
                usuario.getUsuario(),
                usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null,
                usuario.getPerfil()
        );
        List<UUID> setoresIds = alocacaoRepository.findAllByUsuarioId(usuario.getId()).stream()
                .filter(a -> a.getDataFim() == null) // só alocações ainda ativas
                .map(a -> a.getSetor().getId())
                .toList();

        return new LoginResponseDTO(
                token,
                usuario.getNome(),
                usuario.getPerfil(),
                usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null,
                setoresIds
        );
    }
}
