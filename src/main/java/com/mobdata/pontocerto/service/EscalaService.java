package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.EscalaRequestDTO;
import com.mobdata.pontocerto.dto.TurnoRequestDTO;
import com.mobdata.pontocerto.model.Empresa;
import com.mobdata.pontocerto.model.Escala;
import com.mobdata.pontocerto.model.ModelEscala;
import com.mobdata.pontocerto.model.Turno;
import com.mobdata.pontocerto.repository.EmpresaRepository;
import com.mobdata.pontocerto.repository.EscalaRepository;
import com.mobdata.pontocerto.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EscalaService {

    @Autowired
    private EscalaRepository escalaRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    public Escala cadastrar(EscalaRequestDTO request) {
        Empresa empresa = resolverEmpresa(request.empresaId());

        Escala escala = new Escala();
        escala.setEmpresa(empresa);
        preencher(escala, request);

        validar(escala);
        return escalaRepository.save(escala);
    }

    public List<Escala> listar(UUID empresaIdParam) {
        UUID empresaId = TenantContext.isSuperAdmin() ? empresaIdParam : TenantContext.getEmpresaId();

        if (empresaId == null) {
            throw new IllegalArgumentException("Informe a empresa (empresaId) para listar as escalas");
        }

        return escalaRepository.findAllByEmpresaId(empresaId);
    }

    public Escala buscarPorId(UUID id) {
        Escala escala = escalaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Escala não encontrada"));

        verificarAcesso(escala);
        return escala;
    }

    public Escala atualizar(UUID id, EscalaRequestDTO request) {
        Escala escala = buscarPorId(id);
        preencher(escala, request);

        validar(escala);
        return escalaRepository.save(escala);
    }

    public void excluir(UUID id) {
        Escala escala = buscarPorId(id);
        escalaRepository.delete(escala);
    }

    public List<PrevisaoDia> gerarPrevisao(UUID id, LocalDate dataInicio, int quantidadeDias) {
        Escala escala = buscarPorId(id);
        List<PrevisaoDia> previsao = new ArrayList<>();

        for (int i = 0; i < quantidadeDias; i++) {
            LocalDate data = dataInicio.plusDays(i);
            boolean trabalha = ehDiaDeTrabalho(escala, data);
            previsao.add(new PrevisaoDia(data, trabalha, trabalha ? escala.getTurnos() : List.of()));
        }

        return previsao;
    }

    private void preencher(Escala escala, EscalaRequestDTO request) {
        escala.setNome(request.nome());
        escala.setModelo(request.modelo());
        escala.setDataReferencia(request.dataReferencia());
        escala.setDiasSemana(request.diasSemana());

        List<Turno> turnos = new ArrayList<>();
        for (TurnoRequestDTO turnoRequest : request.turnos()) {
            turnos.add(new Turno(turnoRequest.horaInicio(), turnoRequest.horaFim(), turnoRequest.intervaloMinutos()));
        }
        escala.setTurnos(turnos);
    }

    private void validar(Escala escala) {
        if (exigeDataReferencia(escala.getModelo()) && escala.getDataReferencia() == null) {
            throw new IllegalArgumentException(
                    "Escalas 24x72 e 12x36 exigem uma data de referência para o cálculo do ciclo");
        }
    }

    private boolean exigeDataReferencia(ModelEscala modelo) {
        return modelo == ModelEscala.JORNADA_24X72 || modelo == ModelEscala.JORNADA_12X36;
    }

    private boolean ehDiaDeTrabalho(Escala escala, LocalDate data) {
        Integer tamanhoCiclo = tamanhoDoCiclo(escala.getModelo());

        if (tamanhoCiclo != null) {
            if (escala.getDataReferencia() == null) {
                return false;
            }
            long diferencaDias = ChronoUnit.DAYS.between(escala.getDataReferencia(), data);
            long posicao = Math.floorMod(diferencaDias, tamanhoCiclo);
            return posicao == 0;
        }

        var diasSemana = escala.getDiasSemana();

        if (escala.getModelo() == ModelEscala.COMERCIAL_5X2) {
            if (diasSemana != null && !diasSemana.isEmpty()) {
                return diasSemana.contains(data.getDayOfWeek().getValue() % 7);
            }
            int diaSemana = data.getDayOfWeek().getValue();
            return diaSemana >= 1 && diaSemana <= 5;
        }

        if (diasSemana != null && !diasSemana.isEmpty()) {
            return diasSemana.contains(data.getDayOfWeek().getValue() % 7);
        }

        return true;
    }

    private Integer tamanhoDoCiclo(ModelEscala modelo) {
        if (modelo == ModelEscala.JORNADA_24X72) return 4;
        if (modelo == ModelEscala.JORNADA_12X36) return 2;
        return null;
    }

    private Empresa resolverEmpresa(UUID empresaIdDoRequest) {
        UUID empresaId = TenantContext.isSuperAdmin()
                ? empresaIdDoRequest
                : TenantContext.getEmpresaId();

        if (empresaId == null) {
            throw new IllegalArgumentException("Empresa é obrigatória para cadastrar uma escala");
        }

        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
    }

    private void verificarAcesso(Escala escala) {
        if (TenantContext.isSuperAdmin()) {
            return;
        }

        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null || !escala.getEmpresa().getId().equals(empresaId)) {
            throw new IllegalArgumentException("Escala não encontrada");
        }
    }

    public record PrevisaoDia(LocalDate data, boolean trabalha, List<Turno> turnos) {
    }
}