package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AberturaResponseDTO;
import com.mobdata.pontocerto.dto.EspelhoDiaDTO;
import com.mobdata.pontocerto.dto.SituacaoDia;
import com.mobdata.pontocerto.exception.DispositivoNaoRegistradoException;
import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.model.Dispositivo;
import com.mobdata.pontocerto.model.Empresa;
import com.mobdata.pontocerto.model.Escala;
import com.mobdata.pontocerto.model.Marcacao;
import com.mobdata.pontocerto.model.ModelEscala;
import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.PoliticaForaPerimetro;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.model.TipoDispositivo;
import com.mobdata.pontocerto.model.TipoMarcacao;
import com.mobdata.pontocerto.model.Turno;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.AfastamentoRepository;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.DispositivoRepository;
import com.mobdata.pontocerto.repository.FeriadoRepository;
import com.mobdata.pontocerto.repository.MarcacaoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AberturaServiceTest {

    // "Agora" fixo: 15/10/2026 às 10h no horário de Recife
    private static final ZonedDateTime AGORA =
            ZonedDateTime.of(2026, 10, 15, 10, 0, 0, 0, AberturaService.FUSO);
    private static final LocalDate HOJE = AGORA.toLocalDate();

    // Identificador do aparelho ativo da conta (a abertura exige que bata)
    private static final String APARELHO = "aparelho-da-maria";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AlocacaoRepository alocacaoRepository;
    @Mock private MarcacaoRepository marcacaoRepository;
    @Mock private FeriadoRepository feriadoRepository;
    @Mock private AfastamentoRepository afastamentoRepository;
    @Mock private DispositivoRepository dispositivoRepository;

    private AberturaService aberturaService;
    private Usuario usuario;
    private Setor galpao;
    private Setor escritorio;

    @BeforeEach
    void setUp() {
        // EspelhoService real (com repositórios falsos), para testar a agenda de verdade
        EspelhoService espelhoService = new EspelhoService();
        ReflectionTestUtils.setField(espelhoService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(espelhoService, "alocacaoRepository", alocacaoRepository);
        ReflectionTestUtils.setField(espelhoService, "feriadoRepository", feriadoRepository);
        ReflectionTestUtils.setField(espelhoService, "afastamentoRepository", afastamentoRepository);

        aberturaService = new AberturaService();
        ReflectionTestUtils.setField(aberturaService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(aberturaService, "alocacaoRepository", alocacaoRepository);
        ReflectionTestUtils.setField(aberturaService, "marcacaoRepository", marcacaoRepository);
        ReflectionTestUtils.setField(aberturaService, "dispositivoRepository", dispositivoRepository);
        ReflectionTestUtils.setField(aberturaService, "espelhoService", espelhoService);
        aberturaService.setClock(Clock.fixed(AGORA.toInstant(), AberturaService.FUSO));

        Empresa empresa = new Empresa();
        empresa.setId(UUID.randomUUID());
        empresa.setRazaoSocial("Mobdata Ltda");

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNome("Maria Silva");
        usuario.setUsuario("maria");
        usuario.setPerfil(Perfil.FUNCIONARIO);
        usuario.setMatricula("123");
        usuario.setCargo("Conferente");
        usuario.setEmpresa(empresa);

        galpao = setor("Galpão", empresa);
        escritorio = setor("Escritório", empresa);

        lenient().when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        Dispositivo aparelho = new Dispositivo();
        aparelho.setId(UUID.randomUUID());
        aparelho.setTipo(TipoDispositivo.PESSOAL);
        aparelho.setIdentificador(APARELHO);
        aparelho.setUsuario(usuario);
        aparelho.setAtivo(true);
        lenient().when(dispositivoRepository.findByUsuarioIdAndAtivoTrue(usuario.getId()))
                .thenReturn(Optional.of(aparelho));
    }

    private Setor setor(String nome, Empresa empresa) {
        Setor s = new Setor();
        s.setId(UUID.randomUUID());
        s.setNome(nome);
        s.setEndereco("Rua A, 1");
        s.setLatitude(-7.98);
        s.setLongitude(-38.29);
        s.setRaioMetros(150);
        s.setExigirSelfie(true);
        s.setPoliticaForaPerimetro(PoliticaForaPerimetro.PENDENTE_ANALISE);
        s.setIgnorarLocalizacao(false);
        s.setEmpresa(empresa);
        return s;
    }

    private Escala escala(String nome, LocalTime inicio, LocalTime fim) {
        Escala e = new Escala();
        ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
        e.setNome(nome);
        e.setModelo(ModelEscala.TURNO_DIURNO); // sem diasSemana = todo dia
        e.setTurnos(List.of(new Turno(inicio, fim, 60)));
        return e;
    }

    private Alocacao alocacao(Setor setor, Escala escala, LocalDate inicio, LocalDate fim) {
        Alocacao a = new Alocacao();
        a.setId(UUID.randomUUID());
        a.setUsuario(usuario);
        a.setSetor(setor);
        a.setEscala(escala);
        a.setDataInicio(inicio);
        a.setDataFim(fim);
        return a;
    }

    @Test
    void devolveHoraDoServidorEFuso() {
        when(alocacaoRepository.findAllByUsuarioId(usuario.getId())).thenReturn(List.of());

        AberturaResponseDTO r = aberturaService.montar(usuario.getId(), null, APARELHO);

        assertThat(r.horaServidor()).isEqualTo(AGORA.toInstant());
        assertThat(r.horaServidorEpochMs()).isEqualTo(AGORA.toInstant().toEpochMilli());
        assertThat(r.fusoHorario()).isEqualTo("America/Recife");
    }

    @Test
    void periodoVaiDeOntemAteHojeMaisDias() {
        when(alocacaoRepository.findAllByUsuarioId(usuario.getId())).thenReturn(List.of());

        AberturaResponseDTO padrao = aberturaService.montar(usuario.getId(), null, APARELHO);
        assertThat(padrao.periodoInicio()).isEqualTo(HOJE.minusDays(1));
        assertThat(padrao.periodoFim()).isEqualTo(HOJE.plusDays(AberturaService.DIAS_PADRAO));
        // um item de agenda por dia do período
        assertThat(padrao.agenda()).hasSize(AberturaService.DIAS_PADRAO + 2);

        assertThat(aberturaService.montar(usuario.getId(), 500, APARELHO).periodoFim())
                .isEqualTo(HOJE.plusDays(AberturaService.DIAS_MAXIMO));
        assertThat(aberturaService.montar(usuario.getId(), 0, APARELHO).periodoFim())
                .isEqualTo(HOJE.plusDays(1));
    }

    @Test
    void juntaFuncionarioSetoresComPoliticaEscalasSemRepetir() {
        Escala comercial = escala("Comercial", LocalTime.of(8, 0), LocalTime.of(17, 0));
        Escala antiga = escala("Antiga", LocalTime.of(6, 0), LocalTime.of(14, 0));

        when(alocacaoRepository.findAllByUsuarioId(usuario.getId())).thenReturn(List.of(
                // encerrada antes do período: deve ficar de fora
                alocacao(escritorio, antiga, HOJE.minusMonths(2), HOJE.minusMonths(1)),
                // galpão em duas alocações seguidas: setor e escala aparecem uma vez só
                alocacao(galpao, comercial, HOJE.minusDays(10), HOJE.plusDays(3)),
                alocacao(galpao, comercial, HOJE.plusDays(4), HOJE.plusDays(30))
        ));

        AberturaResponseDTO r = aberturaService.montar(usuario.getId(), null, APARELHO);

        assertThat(r.funcionario().nome()).isEqualTo("Maria Silva");
        assertThat(r.funcionario().matricula()).isEqualTo("123");
        assertThat(r.funcionario().cargo()).isEqualTo("Conferente");
        assertThat(r.funcionario().empresaNome()).isEqualTo("Mobdata Ltda");

        assertThat(r.alocacoes()).hasSize(2);
        assertThat(r.setores()).extracting(AberturaResponseDTO.SetorDTO::nome).containsExactly("Galpão");
        assertThat(r.escalas()).extracting(AberturaResponseDTO.EscalaDTO::nome).containsExactly("Comercial");

        var politica = r.setores().get(0).politica();
        assertThat(politica.raioMetros()).isEqualTo(150);
        assertThat(politica.exigirSelfie()).isTrue();
        assertThat(politica.politicaForaPerimetro()).isEqualTo(PoliticaForaPerimetro.PENDENTE_ANALISE);
        assertThat(r.setores().get(0).latitude()).isEqualTo(-7.98);

        assertThat(r.escalas().get(0).turnos()).hasSize(1);
        assertThat(r.escalas().get(0).turnos().get(0).horaInicio()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void agendaIncluiTurnoNoturnoQueComecouOntem() {
        Escala noturna = escala("Noturno", LocalTime.of(22, 0), LocalTime.of(6, 0));
        when(alocacaoRepository.findAllByUsuarioId(usuario.getId())).thenReturn(List.of(
                alocacao(galpao, noturna, HOJE.minusDays(5), HOJE.plusDays(30))
        ));

        AberturaResponseDTO r = aberturaService.montar(usuario.getId(), 3, APARELHO);

        EspelhoDiaDTO ontem = r.agenda().get(0);
        assertThat(ontem.data()).isEqualTo(HOJE.minusDays(1));
        assertThat(ontem.situacao()).isEqualTo(SituacaoDia.TRABALHO);
        assertThat(ontem.turnos().get(0).atravessaMeiaNoite()).isTrue();
        assertThat(ontem.turnos().get(0).setorNome()).isEqualTo("Galpão");
    }

    @Test
    void marcacoesRecentesVemComHoraCorrigidaPeloDesvio() {
        when(alocacaoRepository.findAllByUsuarioId(usuario.getId())).thenReturn(List.of());

        Marcacao m = new Marcacao();
        m.setId(UUID.randomUUID());
        m.setUsuario(usuario);
        m.setSetor(galpao);
        m.setTipo(TipoMarcacao.ENTRADA);
        // celular estava 2 minutos adiantado: marcou 08:02 mas a hora real era 08:00
        Instant real = AGORA.withHour(8).toInstant();
        m.setHoraAparelho(real.plusSeconds(120));
        m.setDesvioRelogioMs(120_000L);
        m.setOffline(true);
        m.setRecebidaEm(AGORA.toInstant());

        Instant seteDiasAtras = HOJE.minusDays(AberturaService.DIAS_MARCACOES_RECENTES)
                .atStartOfDay(AberturaService.FUSO).toInstant();
        when(marcacaoRepository.findTop100ByUsuarioIdAndHoraAparelhoGreaterThanEqualOrderByHoraAparelhoDesc(
                eq(usuario.getId()), eq(seteDiasAtras))).thenReturn(List.of(m));

        AberturaResponseDTO r = aberturaService.montar(usuario.getId(), null, APARELHO);

        assertThat(r.marcacoesRecentes()).hasSize(1);
        assertThat(r.marcacoesRecentes().get(0).tipo()).isEqualTo(TipoMarcacao.ENTRADA);
        assertThat(r.marcacoesRecentes().get(0).dataHora()).isEqualTo(real);
        assertThat(r.marcacoesRecentes().get(0).setorId()).isEqualTo(galpao.getId());
    }

    @Test
    void usuarioInexistenteDaErro() {
        UUID qualquer = UUID.randomUUID();
        when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aberturaService.montar(qualquer, null, APARELHO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aparelhoQueNaoEOAtivoDaContaERecusado() {
        assertThatThrownBy(() -> aberturaService.montar(usuario.getId(), null, "outro-aparelho"))
                .isInstanceOf(DispositivoNaoRegistradoException.class);
    }
}