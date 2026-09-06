package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByUsuario(String usuario);

    boolean existsBySetorId(UUID setorId);
}

