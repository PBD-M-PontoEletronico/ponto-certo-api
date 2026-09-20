package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.EspelhoDiaDTO;
import com.mobdata.pontocerto.dto.EspelhoTurnoDTO;
import com.mobdata.pontocerto.dto.SituacaoDia;
import com.mobdata.pontocerto.model.Afastamento;
import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.model.Feriado;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.AfastamentoRepository;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.FeriadoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Espelho de escala do mês: para cada dia, o que o funcionário devia fazer.
 * Cada dia usa a alocação que VALIA naquela data — por isso o espelho de um
 * mês passado continua igual mesmo que a pessoa tenha trocado de escala
 * depois (a alocação antiga nunca é apagada, só encerrada).
 */
@Service
public class EspelhoService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AlocacaoRepository alocacaoRepository;

    @Autowired
    private FeriadoRepository feriadoRepository;

    @Autowired
    private AfastamentoRepository afastamentoRepository;

    @Transactional(readOnly = true)
    public List<EspelhoDiaDTO> espelhoDoMes(UUID usuarioId, YearMonth mes) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));
        verificarAcessoAoUsuario(usuario);

        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();

        List<Alocacao> alocacoes = alocacaoRepository.findAllByUsuarioId(usuarioId);
        List<Afastamento> afastamentos = afastamentoRepository.findSobrepostos(usuarioId, inicio, fim);
        List<Feriado> feriados = usuario.getEmpresa() == null
                ? List.of()
                : feriadoRepository.findAllByEmpresaIdAndDataBetweenOrderByDataAsc(
                        usuario.getEmpresa().getId(), inicio, fim);

        List<EspelhoDiaDTO> dias = new ArrayList<>();
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            dias.add(montarDia(data, alocacoes, afastamentos, feriados));
        }
        return dias;
    }

    private EspelhoDiaDTO montarDia(LocalDate data, List<Alocacao> alocacoes,
                                    List<Afastamento> afastamentos, List<Feriado> feriados) {
        // Afastamento é da pessoa e vale para todas as alocações do dia
        Optional<Afastamento> afastamento = DispensaCalculo.afastamentoNoDia(afastamentos, data);
        if (afastamento.isPresent()) {
            return new EspelhoDiaDTO(data, SituacaoDia.AFASTAMENTO,
                    afastamento.get().getTipo().getRotulo(), List.of());
        }

        List<Alocacao> ativas = alocacoes.stream()
                .filter(a -> !data.isBefore(a.getDataInicio()) && !data.isAfter(a.getDataFim()))
                .toList();

        List<EspelhoTurnoDTO> turnos = new ArrayList<>();
        Feriado feriadoDoDia = null;

        for (Alocacao alocacao : ativas) {
            // Feriado vale por setor: só dispensa quem está alocado onde ele vale
            Optional<Feriado> feriado = DispensaCalculo.feriadoNoDia(feriados, alocacao.getSetor(), data);
            if (feriado.isPresent()) {
                feriadoDoDia = feriado.get();
                continue;
            }

            for (EscalaCalculo.PeriodoTurno periodo : EscalaCalculo.projetarTurnos(alocacao.getEscala(), data, data)) {
                turnos.add(new EspelhoTurnoDTO(
                        periodo.inicio().toLocalTime(),
                        periodo.fim().toLocalTime(),
                        !periodo.fim().toLocalDate().equals(periodo.inicio().toLocalDate()),
                        alocacao.getSetor().getId(),
                        alocacao.getSetor().getNome(),
                        alocacao.getEscala().getNome()
                ));
            }
        }

        turnos.sort(Comparator.comparing(EspelhoTurnoDTO::horaInicio));

        if (!turnos.isEmpty()) {
            return new EspelhoDiaDTO(data, SituacaoDia.TRABALHO, null, turnos);
        }
        if (feriadoDoDia != null) {
            return new EspelhoDiaDTO(data, SituacaoDia.FERIADO, feriadoDoDia.getDescricao(), List.of());
        }
        if (!ativas.isEmpty()) {
            return new EspelhoDiaDTO(data, SituacaoDia.FOLGA, null, List.of());
        }
        return new EspelhoDiaDTO(data, SituacaoDia.SEM_ALOCACAO, null, List.of());
    }

    private void verificarAcessoAoUsuario(Usuario usuario) {
        if (TenantContext.isSuperAdmin()) {
            return;
        }

        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null || usuario.getEmpresa() == null || !usuario.getEmpresa().getId().equals(empresaId)) {
            throw new IllegalArgumentException("Funcionário não encontrado");
        }
    }
}
