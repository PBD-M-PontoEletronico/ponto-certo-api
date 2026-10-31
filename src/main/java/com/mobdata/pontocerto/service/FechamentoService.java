package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.*;
import com.mobdata.pontocerto.model.*;
import com.mobdata.pontocerto.repository.*;
import com.mobdata.pontocerto.security.TenantContext;
import com.mobdata.pontocerto.specification.FechamentoLogSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fechamento de mês — versão mínima (W10). Um mês fechado não aceita mais
 * pedido de justificativa ou ajuste, nem aprovação de pedido que ainda
 * estava pendente.
 */
@Service
public class FechamentoService {

    private static final ZoneId FUSO = ZoneId.of("America/Recife");
    private static final DateTimeFormatter DATA_HORA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    @Autowired
    private FechamentoPeriodoRepository fechamentoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SaldoFechamentoRepository  saldoFechamentoRepository;

    @Autowired
    private FechamentoLogRepository fechamentoLogRepository;

    @Autowired
    private EspelhoService espelhoService;

    public List<FechamentoPeriodo> listar(UUID empresaIdParam) {
        return fechamentoRepository.findAllByEmpresaIdOrderByAnoDescMesDesc(empresaDaRequisicao(empresaIdParam));
    }

    @Transactional
    public FechamentoPeriodo fechar(FechamentoRequestDTO dto) {
        UUID empresaId = empresaDaRequisicao(dto.empresaId());
        YearMonth mes = YearMonth.parse(dto.mes());

        if (mes.isAfter(YearMonth.now(FUSO))) {
            throw new IllegalArgumentException("Não é possível fechar um mês que ainda não começou");
        }

        Optional<FechamentoPeriodo> existente =
                fechamentoRepository.findByEmpresaIdAndAnoAndMes(empresaId, mes.getYear(), mes.getMonthValue());
        if (existente.isPresent()) {
            throw new IllegalStateException("O mês " + rotulo(mes) + " já está fechado");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
        Usuario quem = usuarioRepository.findById(TenantContext.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Sessão inválida"));

        FechamentoPeriodo fechamento = new FechamentoPeriodo();
        fechamento.setEmpresa(empresa);
        fechamento.setAno(mes.getYear());
        fechamento.setMes(mes.getMonthValue());
        fechamento.setFechadoEm(Instant.now());
        fechamento.setFechadoPor(quem);
        FechamentoPeriodo salvo = fechamentoRepository.save(fechamento);

        for (Usuario funcionario : usuarioRepository.findAllByEmpresaIdAndPerfil(empresaId, Perfil.FUNCIONARIO)) {
            List<EspelhoDiaDTO> dias = espelhoService.espelhoDoMes(funcionario.getId(), mes);
            long saldoFinal = dias.isEmpty() ? 0 : dias.get(dias.size() - 1).saldoAcumuladoMinutos();

            SaldoFechamento snapshot = new SaldoFechamento();
            snapshot.setUsuario(funcionario);
            snapshot.setAnoMes(mes.getYear() * 100 + mes.getMonthValue());
            snapshot.setSaldoAcumuladoMinutos(saldoFinal);
            snapshot.setFechamento(salvo);
            saldoFechamentoRepository.save(snapshot);
        }
        registrarLog(empresa, mes.getYear(), mes.getMonthValue(), AcaoFechamento.FECHADO, quem);
        return salvo;
    }


    public boolean estaFechado(UUID empresaId, LocalDate data) {
        return buscar(empresaId, data).isPresent();
    }

    /**
     * Lança erro com mensagem clara se o mês de "data" já foi fechado.
     * "acao" completa a frase, ex.: "Não é possível registrar o pedido".
     */
    public void exigirAberto(UUID empresaId, LocalDate data, String acao) {
        buscar(empresaId, data).ifPresent(f -> {
            throw new IllegalStateException(
                    acao + ": o mês " + rotulo(YearMonth.from(data)) + " já foi fechado"
                            + " (por " + f.getFechadoPor().getNome()
                            + " em " + DATA_HORA_BR.format(f.getFechadoEm().atZone(FUSO)) + ")."
                            + " Procure o RH se precisar corrigir esse mês."
            );
        });
    }

    private Optional<FechamentoPeriodo> buscar(UUID empresaId, LocalDate data) {
        if (empresaId == null || data == null) {
            return Optional.empty();
        }
        return fechamentoRepository.findByEmpresaIdAndAnoAndMes(empresaId, data.getYear(), data.getMonthValue());
    }

    private UUID empresaDaRequisicao(UUID empresaIdParam) {
        UUID empresaId = TenantContext.isSuperAdmin() ? empresaIdParam : TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new IllegalArgumentException("Informe a empresa (empresaId)");
        }
        return empresaId;
    }

    @Transactional
    public void reabrir(UUID id) {
        FechamentoPeriodo fechamento = fechamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fechamento não encontrado"));

        if (!TenantContext.isSuperAdmin() && !fechamento.getEmpresa().getId().equals(TenantContext.getEmpresaId())) {
            throw new IllegalArgumentException("Fechamento não encontrado");
        }

        Usuario quem = usuarioRepository.findById(TenantContext.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Sessão inválida"));
        registrarLog(fechamento.getEmpresa(), fechamento.getAno(), fechamento.getMes(), AcaoFechamento.REABERTO, quem);

        saldoFechamentoRepository.deleteByFechamentoId(fechamento.getId());
        fechamentoRepository.delete(fechamento);
    }

    public PaginaDTO<FechamentoLogResponseDTO> historico(FechamentoLogFiltroDTO filtro, Pageable pageable) {
        UUID empresaId = TenantContext.isSuperAdmin() ? filtro.empresaId() : TenantContext.getEmpresaId();

        Specification<FechamentoLog> spec = Specification
                .where(FechamentoLogSpecification.comEmpresaId(empresaId))
                .and(FechamentoLogSpecification.comAno(filtro.ano()))
                .and(FechamentoLogSpecification.comMes(filtro.mes()))
                .and(FechamentoLogSpecification.comAcao(filtro.acao()));

        Page<FechamentoLogResponseDTO> pagina = fechamentoLogRepository.findAll(spec, pageable)
                .map(FechamentoLogResponseDTO::fromEntity);
        return PaginaDTO.fromPage(pagina);
    }

    private void registrarLog(Empresa empresa, int ano, int mes, AcaoFechamento acao, Usuario quem) {
        FechamentoLog log = new FechamentoLog();
        log.setEmpresa(empresa);
        log.setAno(ano);
        log.setMes(mes);
        log.setAcao(acao);
        log.setUsuario(quem);
        log.setQuando(Instant.now());
        fechamentoLogRepository.save(log);
    }

    private static String rotulo(YearMonth mes) {
        return String.format("%02d/%d", mes.getMonthValue(), mes.getYear());
    }
}
