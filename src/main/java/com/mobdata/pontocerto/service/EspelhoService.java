package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.EspelhoDiaDTO;
import com.mobdata.pontocerto.dto.EspelhoJustificativaDTO;
import com.mobdata.pontocerto.dto.EspelhoMarcacaoDTO;
import com.mobdata.pontocerto.dto.EspelhoTurnoDTO;
import com.mobdata.pontocerto.dto.SituacaoDia;
import com.mobdata.pontocerto.model.*;
import com.mobdata.pontocerto.repository.*;
import com.mobdata.pontocerto.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Espelho de escala do mês: para cada dia, o que o funcionário devia fazer.
 * Cada dia usa a alocação que VALIA naquela data — por isso o espelho de um
 * mês passado continua igual mesmo que a pessoa tenha trocado de escala
 * depois (a alocação antiga nunca é apagada, só encerrada).
 *
 * W10 — além do previsto, cada dia traz o realizado: as marcações batidas
 * (originais) e as criadas por ajuste aprovado, separadas para aparecerem
 * lado a lado, e as justificativas pendentes/aprovadas do dia.
 */
@Service
public class EspelhoService {

    private static final ZoneId FUSO = ZoneId.of("America/Recife");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AlocacaoRepository alocacaoRepository;

    @Autowired
    private FeriadoRepository feriadoRepository;

    @Autowired
    private AfastamentoRepository afastamentoRepository;

    @Autowired
    private MarcacaoRepository marcacaoRepository;

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private SaldoFechamentoRepository saldoFechamentoRepository;

    @Transactional(readOnly = true)
    public List<EspelhoDiaDTO> espelhoDoMes(UUID usuarioId, YearMonth mes) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));
        verificarAcessoAoUsuario(usuario);

        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();

        List<Marcacao> marcacoes = marcacoesDoPeriodo(usuarioId, inicio, fim);

        Map<LocalDate, List<EspelhoMarcacaoDTO>> marcacoesPorDia = marcacoes.stream()
                .collect(Collectors.groupingBy(EspelhoService::diaDa,
                        Collectors.mapping(EspelhoMarcacaoDTO::fromEntity, Collectors.toList())));

        Map<LocalDate, List<EspelhoJustificativaDTO>> justificativasPorDia = solicitacaoRepository
                .findAllByUsuarioIdAndTipoAndDataBetweenOrderByDataAsc(usuarioId, TipoSolicitacao.JUSTIFICATIVA, inicio, fim)
                .stream()
                .filter(s -> s.getStatus() != StatusSolicitacao.RECUSADA)
                .collect(Collectors.groupingBy(Solicitacao::getData,
                        Collectors.mapping(EspelhoJustificativaDTO::fromEntity, Collectors.toList())));
        // Jornadas (ENTRADA->SAIDA) atribuídas ao dia da ENTRADA: turno que vira a
        // meia-noite conta no dia em que começou (critério 3).
        Map<LocalDate, List<HorasCalculo.Jornada>> jornadasPorDia = HorasCalculo
                .montarJornadas(marcacoes, FUSO).stream()
                .collect(Collectors.groupingBy(HorasCalculo.Jornada::diaDeInicio));

        int anoMesAtual = mes.getYear() * 100 + mes.getMonthValue();
        long saldoAcumulado = saldoFechamentoRepository
                .findFirstByUsuarioIdAndAnoMesLessThanOrderByAnoMesDesc(usuarioId, anoMesAtual)
                .map(SaldoFechamento::getSaldoAcumuladoMinutos)
                .orElse(0L);

        List<EspelhoDiaDTO> dias = new ArrayList<>();
        for (EspelhoDiaDTO diaBase : agendaDoPeriodo(usuario, inicio, fim)) {
            List<HorasCalculo.Jornada> jornadasDoDia = jornadasPorDia.getOrDefault(diaBase.data(), List.of());
            boolean algumaIncompleta = jornadasDoDia.stream().anyMatch(j -> !j.completa());

            Integer minutosTrabalhados = algumaIncompleta
                    ? null
                    : jornadasDoDia.stream().mapToInt(HorasCalculo.Jornada::minutosTrabalhados).sum();

            Integer minutosAtraso = calcularAtraso(diaBase, jornadasDoDia);

            Integer saldoDoDia = minutosTrabalhados == null ? null : minutosTrabalhados - diaBase.minutosPrevistos();
            saldoAcumulado += (saldoDoDia == null ? 0 : saldoDoDia);

            EspelhoDiaDTO comRealizado = diaBase.comRealizado(
                    marcacoesPorDia.getOrDefault(diaBase.data(), List.of()),
                    justificativasPorDia.getOrDefault(diaBase.data(), List.of()));

            dias.add(comRealizado.comHoras(minutosTrabalhados, minutosAtraso, saldoDoDia, (int) saldoAcumulado));
        }
        return dias;
    }

    private Integer calcularAtraso(EspelhoDiaDTO diaBase, List<HorasCalculo.Jornada> jornadasDoDia) {
        if (diaBase.turnos().isEmpty() || jornadasDoDia.isEmpty()) {
            return null; // sem turno previsto ou sem entrada registrada: não aplicável
        }
        LocalTime inicioPrevisto = diaBase.turnos().get(0).horaInicio();
        Instant entradaMaisAntiga = jornadasDoDia.stream()
                .map(HorasCalculo.Jornada::horaEntrada)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        Instant previsto = LocalDateTime.of(diaBase.data(), inicioPrevisto).atZone(FUSO).toInstant();
        long diffMinutos = Duration.between(previsto, entradaMaisAntiga).toMinutes();
        return (int) Math.max(0, diffMinutos);
    }

    /**
     * Marcações do funcionário cujo horário real (hora corrigida pelo desvio
     * do relógio) cai entre os dias [inicio, fim], em ordem de horário.
     * Busca com folga de um dia de cada lado porque o filtro do banco usa a
     * hora do aparelho, e o desvio pode jogar a marcação para o dia vizinho.
     */
    @Transactional(readOnly = true)
    public List<Marcacao> marcacoesDoPeriodo(UUID usuarioId, LocalDate inicio, LocalDate fim) {
        var de = inicio.minusDays(1).atStartOfDay(FUSO).toInstant();
        var ate = fim.plusDays(2).atStartOfDay(FUSO).toInstant();

        return marcacaoRepository.findAllByUsuarioIdAndHoraAparelhoBetweenOrderByHoraAparelhoAsc(usuarioId, de, ate)
                .stream()
                .filter(m -> !diaDa(m).isBefore(inicio) && !diaDa(m).isAfter(fim))
                .sorted(Comparator.comparing(Marcacao::getHoraCorrigida))
                .toList();
    }

    private static LocalDate diaDa(Marcacao marcacao) {
        return marcacao.getHoraCorrigida().atZone(FUSO).toLocalDate();
    }

    /**
     * Mesmo cálculo do espelho, para um intervalo qualquer [inicio, fim]
     * (inclusive). Não checa permissão: quem chama já deve ter garantido que
     * pode ver esse funcionário (ex.: a abertura usa o próprio usuário logado).
     */
    @Transactional(readOnly = true)
    public List<EspelhoDiaDTO> agendaDoPeriodo(Usuario usuario, LocalDate inicio, LocalDate fim) {
        UUID usuarioId = usuario.getId();

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
        Optional<Afastamento> afastamento = DispensaCalculo.afastamentoNoDia(afastamentos, data);
        if (afastamento.isPresent()) {
            return new EspelhoDiaDTO(data, SituacaoDia.AFASTAMENTO,
                    afastamento.get().getTipo().getRotulo(), List.of(), 0);
        }

        List<Alocacao> ativas = alocacoes.stream()
                .filter(a -> !data.isBefore(a.getDataInicio()) && !data.isAfter(a.getDataFim()))
                .toList();

        List<EspelhoTurnoDTO> turnos = new ArrayList<>();
        int minutosPrevistos = 0;
        Feriado feriadoDoDia = null;

        for (Alocacao alocacao : ativas) {
            Optional<Feriado> feriado = DispensaCalculo.feriadoNoDia(feriados, alocacao.getSetor(), data);
            if (feriado.isPresent()) {
                feriadoDoDia = feriado.get();
                continue;
            }
            if (!EscalaCalculo.ehDiaDeTrabalho(alocacao.getEscala(), data)) {
                continue;
            }

            for (Turno turno : alocacao.getEscala().getTurnos()) {
                minutosPrevistos += turno.duracaoMinutos();
            }

            for (EscalaCalculo.PeriodoTurno periodo : EscalaCalculo.projetarTurnos(alocacao.getEscala(), data, data)) {
                turnos.add(new EspelhoTurnoDTO(
                        periodo.inicio().toLocalTime(), periodo.fim().toLocalTime(),
                        !periodo.fim().toLocalDate().equals(periodo.inicio().toLocalDate()),
                        alocacao.getSetor().getId(), alocacao.getSetor().getNome(), alocacao.getEscala().getNome()
                ));
            }
        }
        turnos.sort(Comparator.comparing(EspelhoTurnoDTO::horaInicio));

        if (!turnos.isEmpty()) return new EspelhoDiaDTO(data, SituacaoDia.TRABALHO, null, turnos, minutosPrevistos);
        if (feriadoDoDia != null) return new EspelhoDiaDTO(data, SituacaoDia.FERIADO, feriadoDoDia.getDescricao(), List.of(), 0);
        if (!ativas.isEmpty()) return new EspelhoDiaDTO(data, SituacaoDia.FOLGA, null, List.of(), 0);
        return new EspelhoDiaDTO(data, SituacaoDia.SEM_ALOCACAO, null, List.of(), 0);
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
