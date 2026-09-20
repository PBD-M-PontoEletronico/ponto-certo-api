package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FeriadoRepository extends JpaRepository<Feriado, UUID> {

    List<Feriado> findAllByEmpresaIdOrderByDataAsc(UUID empresaId);

    List<Feriado> findAllByEmpresaIdAndDataBetweenOrderByDataAsc(UUID empresaId, LocalDate inicio, LocalDate fim);

    boolean existsByEmpresaIdAndDataAndSetorIsNull(UUID empresaId, LocalDate data);

    boolean existsByEmpresaIdAndDataAndSetorId(UUID empresaId, LocalDate data, UUID setorId);
}
