package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Afastamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AfastamentoRepository extends JpaRepository<Afastamento, UUID> {

    List<Afastamento> findAllByUsuarioIdOrderByDataInicioDesc(UUID usuarioId);

    List<Afastamento> findAllByUsuarioEmpresaIdOrderByDataInicioDesc(UUID empresaId);

    // Afastamentos do funcionário que tocam o intervalo [inicio, fim] (extremos
    // inclusive). Serve tanto para recusar sobreposição no cadastro quanto para
    // buscar o que vale numa janela (agenda e espelho).
    @Query("select a from Afastamento a "
            + "where a.usuario.id = :usuarioId and a.dataInicio <= :fim and a.dataFim >= :inicio")
    List<Afastamento> findSobrepostos(@Param("usuarioId") UUID usuarioId,
                                      @Param("inicio") LocalDate inicio,
                                      @Param("fim") LocalDate fim);
}
