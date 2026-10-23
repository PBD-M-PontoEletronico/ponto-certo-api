package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AberturaResponseDTO;
import com.mobdata.pontocerto.dto.AberturaResponseDTO.AlocacaoDTO;
import com.mobdata.pontocerto.dto.AberturaResponseDTO.EscalaDTO;
import com.mobdata.pontocerto.dto.AberturaResponseDTO.FuncionarioDTO;
import com.mobdata.pontocerto.dto.AberturaResponseDTO.FuncionarioResumoDTO;
import com.mobdata.pontocerto.dto.AberturaResponseDTO.MarcacaoRecenteDTO;
import com.mobdata.pontocerto.dto.AberturaResponseDTO.SetorDTO;
import com.mobdata.pontocerto.dto.EspelhoDiaDTO;
import com.mobdata.pontocerto.exception.DispositivoNaoRegistradoException;
import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.model.Escala;
import com.mobdata.pontocerto.model.ModoAberturaDispositivo;
import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.DispositivoRepository;
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
 * sem rede: dados do funcionário (ou, no modo relógio de setor, a lista de
 * funcionários do setor), setores, escalas com turnos, agenda dia a dia do
 * período, marcações recentes e a hora do servidor.
 */
@Service
public class AberturaService {

    public static final int DIAS_PADRAO = 14;
    public static final int DIAS_MAXIMO = 45;
    public static final int DIAS_MARCACOES_RECENTES = 7;

    public static final ZoneId FUSO = ZoneId.of("America/Recife");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AlocacaoRepository alocacaoRepository;

    @Autowired
    private MarcacaoRepository marcacaoRepository;

    @Autowired
    private DispositivoRepository dispositivoRepository;

    @Autowired
    private EspelhoService espelhoService;

    private Clock clock = Clock.system(FUSO);

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AberturaResponseDTO montar(UUID usuarioId, Integer dias, String identificadorDispositivo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        // Item 6: a chamada só é aceita vindo do dispositivo ativo dessa conta.
        dispositivoRepository.findByUsuarioIdAndAtivoTrue(usuarioId)
                .filter(d -> d.getIdentificador().equals(identificadorDispositivo))
                .orElseThrow(() -> new DispositivoNaoRegistradoException(
                        "Dispositivo não registrado ou não é mais o ativo"));

        LocalDate hoje = LocalDate.now(clock);
        int diasAFrente = normalizarDias(dias);
        // Começa em "ontem" para pegar o turno da noite que ainda está rolando
        // depois da meia-noite (ex.: 22h–06h iniciado ontem).
        LocalDate inicio = hoje.minusDays(1);
        LocalDate fim = hoje.plusDays(diasAFrente);
        Instant agora = clock.instant();

        if (usuario.getPerfil() == Perfil.USUARIO_SETOR) {
            return montarModoRelogio(usuario, hoje, inicio, fim, agora);
        }
        return montarModoPessoal(usuario, inicio, fim, agora);
    }

    private AberturaResponseDTO montarModoPessoal(Usuario usuario, LocalDate inicio, LocalDate fim, Instant agora) {
        List<Alocacao> alocacoes = alocacaoRepository.findAllByUsuarioId(usuario.getId()).stream()
                .filter(a -> !a.getDataFim().isBefore(inicio) && !a.getDataInicio().isAfter(fim))
                .sorted(Comparator.comparing(Alocacao::getDataInicio))
                .toList();

        Map<UUID, Setor> setores = new LinkedHashMap<>();
        Map<UUID, Escala> escalas = new LinkedHashMap<>();
        for (Alocacao a : alocacoes) {
            setores.putIfAbsent(a.getSetor().getId(), a.getSetor());
            escalas.putIfAbsent(a.getEscala().getId(), a.getEscala());
        }

        List<EspelhoDiaDTO> agenda = espelhoService.agendaDoPeriodo(usuario, inicio, fim);

        LocalDate hoje = LocalDate.now(clock);
        Instant desde = hoje.minusDays(DIAS_MARCACOES_RECENTES).atStartOfDay(FUSO).toInstant();
        List<MarcacaoRecenteDTO> marcacoes = marcacaoRepository
                .findTop100ByUsuarioIdAndHoraAparelhoGreaterThanEqualOrderByHoraAparelhoDesc(usuario.getId(), desde)
                .stream()
                .map(MarcacaoRecenteDTO::fromEntity)
                .toList();

        return new AberturaResponseDTO(
                agora, agora.toEpochMilli(), FUSO.getId(), inicio, fim,
                ModoAberturaDispositivo.PESSOAL,
                FuncionarioDTO.fromEntity(usuario),
                List.of(),
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

    private AberturaResponseDTO montarModoRelogio(Usuario contaRelogio, LocalDate hoje, LocalDate inicio,
                                                  LocalDate fim, Instant agora) {
        Setor setor = contaRelogio.getSetor();
        if (setor == null) {
            throw new IllegalArgumentException("Esta conta de setor não está vinculada a nenhum setor");
        }

        List<UUID> idsFuncionarios = alocacaoRepository
                .findAllBySetorIdAndDataFimGreaterThanEqual(setor.getId(), hoje).stream()
                .filter(a -> !a.getDataInicio().isAfter(hoje))
                .map(a -> a.getUsuario().getId())
                .distinct()
                .toList();

        List<FuncionarioResumoDTO> funcionarios = usuarioRepository.findAllById(idsFuncionarios).stream()
                .map(FuncionarioResumoDTO::fromEntity)
                .sorted(Comparator.comparing(FuncionarioResumoDTO::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new AberturaResponseDTO(
                agora, agora.toEpochMilli(), FUSO.getId(), inicio, fim,
                ModoAberturaDispositivo.RELOGIO_SETOR,
                null,
                funcionarios,
                List.of(SetorDTO.fromEntity(setor)),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    static int normalizarDias(Integer dias) {
        if (dias == null) {
            return DIAS_PADRAO;
        }
        return Math.max(1, Math.min(dias, DIAS_MAXIMO));
    }
}