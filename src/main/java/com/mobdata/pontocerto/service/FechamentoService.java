package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.FechamentoRequestDTO;
import com.mobdata.pontocerto.model.Empresa;
import com.mobdata.pontocerto.model.FechamentoPeriodo;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.EmpresaRepository;
import com.mobdata.pontocerto.repository.FechamentoPeriodoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
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
        return fechamentoRepository.save(fechamento);
    }

    // Reabrir = apagar o registro de fechamento.
    @Transactional
    public void reabrir(UUID id) {
        FechamentoPeriodo fechamento = fechamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fechamento não encontrado"));

        if (!TenantContext.isSuperAdmin() && !fechamento.getEmpresa().getId().equals(TenantContext.getEmpresaId())) {
            throw new IllegalArgumentException("Fechamento não encontrado");
        }
        fechamentoRepository.delete(fechamento);
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

    private static String rotulo(YearMonth mes) {
        return String.format("%02d/%d", mes.getMonthValue(), mes.getYear());
    }
}
