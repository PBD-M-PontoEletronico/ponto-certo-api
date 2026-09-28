package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispositivoRepository extends JpaRepository<Dispositivo, UUID>, JpaSpecificationExecutor<Dispositivo> {

    Optional<Dispositivo> findByUsuarioIdAndAtivoTrue(UUID usuarioId);

    List<Dispositivo> findAllByUsuario_Empresa_IdOrderByUltimoAcessoDesc(UUID empresaId);
}