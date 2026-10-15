package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AberturaResponseDTO;
import com.mobdata.pontocerto.dto.AberturaResponseDTO.AlocacaoDTO;
import com.mobdata.pontocerto.dto.AberturaResponseDTO.EscalaDTO;
import com.mobdata.pontocerto.dto.AberturaResponseDTO.FuncionarioDTO;
import com.mobdata.pontocerto.dto.AberturaResponseDTO.MarcacaoRecenteDTO;
import com.mobdata.pontocerto.dto.AberturaResponseDTO.SetorDTO;
import com.mobdata.pontocerto.dto.EspelhoDiaDTO;
import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.model.Escala;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.MarcacaoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * W08 — Abertura do aplicativo.
 *
 * Junta, numa resposta só, o que o aparelho precisa guardar para bater ponto
 * sem rede: dados do funcionário, setores (com localização e política),
 * escalas com turnos, agenda dia a dia do período, marcações recentes e a hora
 * do servidor (para o app calcular o desvio do próprio relógio).
 */
@Service
public class AberturaService {

    /** Dias de agenda à frente quando o app não informa. */
    public static final int DIAS_PADRAO = 14;
    /** Limite de dias à frente, para a resposta não ficar enorme. */
    public static final int DIAS_MAXIMO = 45;
    /** Quantos dias para trás entram nas marcações recentes. */
    public static final int DIAS_MARCACOES_RECENTES = 7;

    // Fuso usado para decidir o que é "hoje". O servidor de produção (Render)
    // roda em UTC; sem isso, depois das 21h daqui o "hoje" já seria amanhã.
    public static final ZoneId FUSO = ZoneId.of("America/Recife");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AlocacaoRepository alocacaoRepository;

    @Autowired
    private MarcacaoRepository marcacaoRepository;

    @Autowired
    private EspelhoService espelhoService;

    // Trocável nos testes para fixar "agora".
    private Clock clock = Clock.system(FUSO);

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AberturaResponseDTO montar(UUID usuarioId, Integer dias) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        int diasAFrente = normalizarDias(dias);
        LocalDate hoje = LocalDate.now(clock);
        // Começa em "ontem" para pegar o turno da noite que ainda está rolando
        // depois da meia-noite (ex.: 22h–06h iniciado ontem).
        LocalDate inicio = hoje.minusDays(1);
        LocalDate fim = hoje.plusDays(diasAFrente);

        // Alocações que tocam o período (as antigas/futuras fora dele não interessam)
        List<Alocacao> alocacoes = alocacaoRepository.findAllByUsuarioId(usuarioId).stream()
                .filter(a -> !a.getDataFim().isBefore(inicio) && !a.getDataInicio().isAfter(fim))
                .sorted(Comparator.comparing(Alocacao::getDataInicio))
                .toList();

        // Setores e escalas sem repetição (mesmo setor pode aparecer em 2 alocações)
        Map<UUID, Setor> setores = new LinkedHashMap<>();
        Map<UUID, Escala> escalas = new LinkedHashMap<>();
        for (Alocacao a : alocacoes) {
            setores.putIfAbsent(a.getSetor().getId(), a.getSetor());
            escalas.putIfAbsent(a.getEscala().getId(), a.getEscala());
        }

        List<EspelhoDiaDTO> agenda = espelhoService.agendaDoPeriodo(usuario, inicio, fim);

        Instant desde = hoje.minusDays(DIAS_MARCACOES_RECENTES).atStartOfDay(FUSO).toInstant();
        List<MarcacaoRecenteDTO> marcacoes = marcacaoRepository
                .findTop100ByUsuarioIdAndHoraAparelhoGreaterThanEqualOrderByHoraAparelhoDesc(usuarioId, desde)
                .stream()
                .map(MarcacaoRecenteDTO::fromEntity)
                .toList();

        // A hora do servidor é lida por último, o mais perto possível do envio,
        // para o desvio calculado no app ser o mais fiel possível.
        Instant agora = clock.instant();

        return new AberturaResponseDTO(
                agora,
                agora.toEpochMilli(),
                FUSO.getId(),
                inicio,
                fim,
                FuncionarioDTO.fromEntity(usuario),
                setores.values().stream()
                        .sorted(Comparator.comparing(Setor::getNome, String.CASE_INSENSITIVE_ORDER))
                        .map(SetorDTO::fromEntity)
                        .toList(),
                escalas.values().stream().map(EscalaDTO::fromEntity).toList(),
                alocacoes.stream().map(AlocacaoDTO::fromEntity).toList(),
                agenda,
                marcacoes
        );
    }

    static int normalizarDias(Integer dias) {
        if (dias == null) {
            return DIAS_PADRAO;
        }
        return Math.max(1, Math.min(dias, DIAS_MAXIMO));
    }
}
