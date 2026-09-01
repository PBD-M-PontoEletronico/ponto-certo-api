package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.UsuarioRequestDTO;
import com.mobdata.pontocerto.model.Empresa;
import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.EmpresaRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Usuario cadastrar(UsuarioRequestDTO request) {
        if (usuarioRepository.findByUsuario(request.usuario()).isPresent()) {
            throw new IllegalArgumentException("Usuário já cadastrado");
        }

        // Todo perfil, exceto SUPERADMIN, precisa de empresa
        Empresa empresa = null;
        if (request.perfil() != Perfil.SUPERADMIN) {
            if (request.empresaId() == null) {
                throw new IllegalArgumentException("Empresa é obrigatória para esse perfil");
            }
            empresa = empresaRepository.findById(request.empresaId())
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setUsuario(request.usuario());
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(request.perfil());
        usuario.setEmpresa(empresa);

        return usuarioRepository.save(usuario);
    }
}
