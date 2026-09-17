package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByUsuario(String usuario);
    List<Usuario> findAllByEmpresaId(UUID empresaId);
    boolean existsByMatriculaAndEmpresaId(String matricula, UUID empresaId);
}

