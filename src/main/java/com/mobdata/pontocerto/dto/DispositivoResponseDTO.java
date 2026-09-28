package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Dispositivo;
import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.TipoDispositivo;

import java.time.LocalDateTime;
import java.util.UUID;

public record DispositivoResponseDTO(
        UUID id,
        TipoDispositivo tipo,
        String identificador,
        UUID usuarioId,
        String usuarioNome,
        Perfil usuarioPerfil,
        UUID empresaId,
        String empresaNome,
        UUID setorId,
        String setorNome,
        boolean ativo,
        LocalDateTime dataVinculo,
        LocalDateTime ultimoAcesso
) {
    public static DispositivoResponseDTO fromEntity(Dispositivo d) {
        var usuario = d.getUsuario();
        var setor = usuario.getSetor();
        var empresa = usuario.getEmpresa();
        return new DispositivoResponseDTO(
                d.getId(),
                d.getTipo(),
                d.getIdentificador(),
                usuario.getId(),
                usuario.getNome(),
                usuario.getPerfil(),
                empresa != null ? empresa.getId() : null,
                empresa != null ? empresa.getRazaoSocial() : null,
                setor != null ? setor.getId() : null,
                setor != null ? setor.getNome() : null,
                d.isAtivo(),
                d.getDataVinculo(),
                d.getUltimoAcesso()
        );
    }
}