package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AnalisarSolicitacaoDTO;
import com.mobdata.pontocerto.dto.SolicitacaoForm;
import com.mobdata.pontocerto.model.*;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.MarcacaoRepository;
import com.mobdata.pontocerto.repository.SolicitacaoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitacaoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Recife");

    @Mock
    private SolicitacaoRepository solicitacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MarcacaoRepository marcacaoRepository;

    @Mock
    private AlocacaoRepository alocacaoRepository;

    @Mock
    private AnexoStorageService anexoStorageService;

    @Mock
    private FechamentoService fechamentoService;

    @Mock
    private EspelhoService espelhoService;

    @InjectMocks
    private SolicitacaoService solicitacaoService;

    private Empresa empresa;
    private Usuario funcionario;
    private Usuario gestor;
    private final LocalDate ontem = LocalDate.now(FUSO).minusDays(1);

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(UUID.randomUUID());

        funcionario = novoUsuario("Ana Funcionária", Perfil.FUNCIONARIO);
        gestor = novoUsuario("Gil Gestor", Perfil.GESTOR);

        lenient().when(usuarioRepository.findById(funcionario.getId())).thenReturn(Optional.of(funcionario));
        lenient().when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
        lenient().when(solicitacaoRepository.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(marcacaoRepository.save(any(Marcacao.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    // ---------------------------------------------------------------- criar

    @Test
    void funcionarioCriaAjusteParaSiMesmoEGuardaQuemPediu() {
        logarComo(funcionario);

        Solicitacao pedido = solicitacaoService.criar(formAjuste(null));

        assertThat(pedido.getStatus()).isEqualTo(StatusSolicitacao.PENDENTE);
        assertThat(pedido.getUsuario()).isSameAs(funcionario);
        assertThat(pedido.getSolicitante()).isSameAs(funcionario);
        assertThat(pedido.getCriadoEm()).isNotNull();
        assertThat(pedido.getHoraPretendida()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void rhRegistraEmNomeDoFuncionarioEOSolicitanteFicaSendoORh() {
        Usuario rh = novoUsuario("Rita RH", Perfil.RH_ADMIN);
        when(usuarioRepository.findById(rh.getId())).thenReturn(Optional.of(rh));
        logarComo(rh);

        Solicitacao pedido = solicitacaoService.criar(formAjuste(funcionario.getId()));

        assertThat(pedido.getUsuario()).isSameAs(funcionario);
        assertThat(pedido.getSolicitante()).isSameAs(rh);
    }

    @Test
    void funcionarioNaoPodePedirEmNomeDeOutro() {
        Usuario colega = novoUsuario("Caio Colega", Perfil.FUNCIONARIO);
        logarComo(funcionario);

        assertThatThrownBy(() -> solicitacaoService.criar(formAjuste(colega.getId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("você mesmo");
    }

    @Test
    void pedidoDePeriodoFechadoERecusadoComMensagemClara() {
        logarComo(funcionario);
        doThrow(new IllegalStateException("Pedido recusado: o mês 09/2026 já foi fechado"))
                .when(fechamentoService).exigirAberto(eq(empresa.getId()), eq(ontem), anyString());

        assertThatThrownBy(() -> solicitacaoService.criar(formAjuste(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já foi fechado");
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void justificativaSemOcorrenciaERecusada() {
        logarComo(funcionario);
        SolicitacaoForm form = new SolicitacaoForm();
        form.setTipo(TipoSolicitacao.JUSTIFICATIVA);
        form.setData(ontem);
        form.setMotivo("Consulta médica");

        assertThatThrownBy(() -> solicitacaoService.criar(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("atraso, falta ou saída antecipada");
    }

    @Test
    void naoAceitaDataFutura() {
        logarComo(funcionario);
        SolicitacaoForm form = formAjuste(null);
        form.setData(LocalDate.now(FUSO).plusDays(1));

        assertThatThrownBy(() -> solicitacaoService.criar(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data futura");
    }

    // ------------------------------------------------------------- analisar

    @Test
    void ajusteAprovadoGeraMarcacaoNovaLigadaAoPedidoEGuardaQuemAnalisou() {
        Solicitacao pedido = pedidoPendenteDeAjuste();
        when(solicitacaoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(alocacaoRepository.findAllByUsuarioId(funcionario.getId())).thenReturn(List.of());
        logarComo(gestor);

        Solicitacao analisado = solicitacaoService.analisar(pedido.getId(),
                new AnalisarSolicitacaoDTO(StatusSolicitacao.APROVADA, "  Conferido com o supervisor  "));

        ArgumentCaptor<Marcacao> captor = ArgumentCaptor.forClass(Marcacao.class);
        verify(marcacaoRepository).save(captor.capture());
        Marcacao nova = captor.getValue();

        assertThat(nova.getSolicitacao()).isSameAs(pedido);
        assertThat(nova.isAjuste()).isTrue();
        assertThat(nova.getTipo()).isEqualTo(TipoMarcacao.ENTRADA);
        assertThat(nova.getStatus()).isEqualTo(StatusMarcacao.VALIDA);
        assertThat(nova.getHoraCorrigida())
                .isEqualTo(LocalDateTime.of(ontem, LocalTime.of(8, 0)).atZone(FUSO).toInstant());

        assertThat(analisado.getStatus()).isEqualTo(StatusSolicitacao.APROVADA);
        assertThat(analisado.getMarcacaoGerada()).isSameAs(nova);
        assertThat(analisado.getParecer()).isEqualTo("Conferido com o supervisor");
        assertThat(analisado.getAnalisadoPor()).isSameAs(gestor);
        assertThat(analisado.getAnalisadoEm()).isNotNull();
    }

    @Test
    void recusaNaoCriaMarcacao() {
        Solicitacao pedido = pedidoPendenteDeAjuste();
        when(solicitacaoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        logarComo(gestor);

        Solicitacao analisado = solicitacaoService.analisar(pedido.getId(),
                new AnalisarSolicitacaoDTO(StatusSolicitacao.RECUSADA, "Sem registro na portaria"));

        assertThat(analisado.getStatus()).isEqualTo(StatusSolicitacao.RECUSADA);
        assertThat(analisado.getMarcacaoGerada()).isNull();
        verify(marcacaoRepository, never()).save(any());
    }

    @Test
    void pedidoJaAnalisadoNaoPodeSerAnalisadoDeNovo() {
        Solicitacao pedido = pedidoPendenteDeAjuste();
        pedido.setStatus(StatusSolicitacao.APROVADA);
        pedido.setAnalisadoPor(gestor);
        when(solicitacaoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        logarComo(gestor);

        assertThatThrownBy(() -> solicitacaoService.analisar(pedido.getId(),
                new AnalisarSolicitacaoDTO(StatusSolicitacao.RECUSADA, "Mudei de ideia")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já foi aprovado");
    }

    @Test
    void gestorNaoAnalisaOProprioPedido() {
        Solicitacao pedido = pedidoPendenteDeAjuste();
        pedido.setUsuario(gestor);
        when(solicitacaoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        logarComo(gestor);

        assertThatThrownBy(() -> solicitacaoService.analisar(pedido.getId(),
                new AnalisarSolicitacaoDTO(StatusSolicitacao.APROVADA, "ok")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pedido seu");
    }

    @Test
    void gestorDeOutraEmpresaNaoEncontraOPedido() {
        Solicitacao pedido = pedidoPendenteDeAjuste();
        when(solicitacaoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        TenantContext.set(UUID.randomUUID(), Perfil.GESTOR, gestor.getId());

        assertThatThrownBy(() -> solicitacaoService.analisar(pedido.getId(),
                new AnalisarSolicitacaoDTO(StatusSolicitacao.APROVADA, "ok")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pedido não encontrado");
    }

    @Test
    void naoAprovaPedidoDeMesQueFechouDepois() {
        Solicitacao pedido = pedidoPendenteDeAjuste();
        when(solicitacaoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        doThrow(new IllegalStateException("Não é possível aprovar: o mês já foi fechado"))
                .when(fechamentoService).exigirAberto(eq(empresa.getId()), eq(ontem), anyString());
        logarComo(gestor);

        assertThatThrownBy(() -> solicitacaoService.analisar(pedido.getId(),
                new AnalisarSolicitacaoDTO(StatusSolicitacao.APROVADA, "ok")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já foi fechado");
        verify(marcacaoRepository, never()).save(any());
    }

    // ------------------------------------------------------------ auxiliares

    private Usuario novoUsuario(String nome, Perfil perfil) {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setNome(nome);
        u.setPerfil(perfil);
        u.setEmpresa(empresa);
        return u;
    }

    private void logarComo(Usuario usuario) {
        TenantContext.set(empresa.getId(), usuario.getPerfil(), usuario.getId());
    }

    private SolicitacaoForm formAjuste(UUID usuarioId) {
        SolicitacaoForm form = new SolicitacaoForm();
        form.setUsuarioId(usuarioId);
        form.setTipo(TipoSolicitacao.AJUSTE);
        form.setData(ontem);
        form.setTipoMarcacao(TipoMarcacao.ENTRADA);
        form.setHoraPretendida(LocalTime.of(8, 0));
        form.setMotivo("Esqueci de bater a entrada");
        return form;
    }

    private Solicitacao pedidoPendenteDeAjuste() {
        Solicitacao s = new Solicitacao();
        s.setId(UUID.randomUUID());
        s.setTipo(TipoSolicitacao.AJUSTE);
        s.setUsuario(funcionario);
        s.setSolicitante(funcionario);
        s.setData(ontem);
        s.setTipoMarcacao(TipoMarcacao.ENTRADA);
        s.setHoraPretendida(LocalTime.of(8, 0));
        s.setMotivo("Esqueci de bater a entrada");
        s.setStatus(StatusSolicitacao.PENDENTE);
        return s;
    }
}
