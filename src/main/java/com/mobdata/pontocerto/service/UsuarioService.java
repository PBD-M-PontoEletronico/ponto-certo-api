package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.UsuarioRequestDTO;
import com.mobdata.pontocerto.dto.UsuarioResponseDTO;
import com.mobdata.pontocerto.model.Empresa;
import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.EmpresaRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
        Empresa empresa = null;
        if (request.perfil() != Perfil.SUPERADMIN) {
            UUID empresaId = TenantContext.isSuperAdmin()
                    ? request.empresaId()               // superadmin pode escolher a empresa
                    : TenantContext.getEmpresaId();      // RH_ADMIN só cadastra na própria empresa

            if (empresaId == null) {
                throw new IllegalArgumentException("Empresa é obrigatória para esse perfil");
            }

            empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
        }
        if (request.perfil() == Perfil.FUNCIONARIO) {
            if (request.matricula() == null || request.matricula().isBlank()) {
                throw new IllegalArgumentException("Matrícula é obrigatória para funcionários");
            }
            if (usuarioRepository.existsByMatriculaAndEmpresaId(request.matricula(), empresa.getId())) {
                throw new IllegalArgumentException(
                        "Matrícula \"" + request.matricula() + "\" já cadastrada nesta empresa"
                );
            }
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setUsuario(request.usuario());
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(request.perfil());
        usuario.setEmpresa(empresa);
        usuario.setMatricula(request.matricula()); // NOVO
        usuario.setCargo(request.cargo());

        return usuarioRepository.save(usuario);
    }

    public List<UsuarioResponseDTO> listar(UUID empresaIdParam) {
        UUID empresaId = TenantContext.isSuperAdmin() ? empresaIdParam : TenantContext.getEmpresaId();

        if (empresaId == null) {
            throw new IllegalArgumentException("Informe a empresa (empresaId) para listar os usuários");
        }

        return usuarioRepository.findAllByEmpresaId(empresaId).stream()
                .map(u -> new UsuarioResponseDTO(
                        u.getId(), u.getNome(), u.getUsuario(), u.getPerfil(),
                        u.getEmpresa() != null ? u.getEmpresa().getId() : null,
                        u.getMatricula(), u.getCargo()
                ))
                .toList();
    }
}