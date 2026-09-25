package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.PreferenciaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PreferenciaUsuarioRepository extends JpaRepository<PreferenciaUsuario, UUID> {

    Optional<PreferenciaUsuario> findByUsuarioId(UUID usuarioId);
}
