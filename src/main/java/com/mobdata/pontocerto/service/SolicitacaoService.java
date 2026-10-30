package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.*;
import com.mobdata.pontocerto.model.*;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.MarcacaoRepository;
import com.mobdata.pontocerto.repository.SolicitacaoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import com.mobdata.pontocerto.specification.SolicitacaoSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * W10 — justificativas e solicitações de ajuste.
 *
 * Regra de ouro: nada do que aconteceu é apagado. Aprovar um AJUSTE cria uma
 * marcação NOVA ligada ao pedido; a original (ou a falta dela) continua lá e
 * o espelho mostra as duas lado a lado.
 */
@Service
public class SolicitacaoService {

    private static final ZoneId FUSO = ZoneId.of("America/Recife");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    // Quem pode analisar pedidos e registrar pedido em nome de outra pessoa.
    private static final Set<Perfil> ANALISTAS = Set.of(Perfil.SUPERADMIN, Perfil.RH_ADMIN, Perfil.GESTOR);

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MarcacaoRepository marcacaoRepository;

    @Autowired
    private AlocacaoRepository alocacaoRepository;

    @Autowired
    private AnexoStorageService anexoStorageService;

    @Autowired
    private FechamentoService fechamentoService;

    @Autowired
    private EspelhoService espelhoService;

    // ------------------------------------------------------------------ criar

    @Transactional
    public Solicitacao criar(SolicitacaoForm form) {
        Usuario logado = usuarioLogado();
        Usuario funcionario = resolverFuncionario(form.getUsuarioId(), logado);

        LocalDate data = form.getData();
        LocalDate hoje = LocalDate.now(FUSO);
        if (data.isAfter(hoje)) {
            throw new IllegalArgumentException("Não é possível fazer pedido para uma data futura");
        }

        Solicitacao pedido = new Solicitacao();
        pedido.setTipo(form.getTipo());
        pedido.setUsuario(funcionario);
        pedido.setSolicitante(logado);
        pedido.setData(data);
        pedido.setMotivo(form.getMotivo().trim());

        if (form.getTipo() == TipoSolicitacao.JUSTIFICATIVA) {
            if (form.getOcorrencia() == null) {
                throw new IllegalArgumentException("Informe o que está sendo justificado: atraso, falta ou saída antecipada");
            }
            pedido.setOcorrencia(form.getOcorrencia());
        } else {
            if (form.getTipoMarcacao() == null) {
                throw new IllegalArgumentException("Informe qual marcação ficou faltando (entrada, saída...)");
            }
            if (form.getHoraPretendida() == null) {
                throw new IllegalArgumentException("Informe o horário pretendido da marcação");
            }
            if (LocalDateTime.of(data, form.getHoraPretendida()).isAfter(LocalDateTime.now(FUSO))) {
                throw new IllegalArgumentException("O horário pretendido ainda não chegou");
            }
            pedido.setTipoMarcacao(form.getTipoMarcacao());
            pedido.setHoraPretendida(form.getHoraPretendida());
        }

        // Mês já fechado não aceita pedido novo
        fechamentoService.exigirAberto(funcionario.getEmpresa().getId(), data, "Pedido recusado");

        validarSemPedidoRepetido(pedido);

        pedido.setStatus(StatusSolicitacao.PENDENTE);
        pedido.setCriadoEm(Instant.now());

        AnexoStorageService.AnexoSalvo anexoSalvo = null;
        MultipartFile anexo = form.getAnexo();
        if (anexo != null && !anexo.isEmpty()) {
            anexoSalvo = anexoStorageService.salvar(anexo, AnexoStorageService.SOLICITACOES);
            pedido.setAnexoArquivo(anexoSalvo.arquivo());
            pedido.setAnexoNomeOriginal(anexoSalvo.nomeOriginal());
            pedido.setAnexoTipoConteudo(anexoSalvo.tipoConteudo());
        }

        try {
            return solicitacaoRepository.save(pedido);
        } catch (RuntimeException e) {
            if (anexoSalvo != null) {
                anexoStorageService.remover(anexoSalvo.arquivo(), AnexoStorageService.SOLICITACOES);
            }
            throw e;
        }
    }

    // -------------------------------------------------------------- analisar

    @Transactional
    public Solicitacao analisar(UUID id, AnalisarSolicitacaoDTO dto) {
        Usuario analista = usuarioLogado();
        if (!ANALISTAS.contains(analista.getPerfil())) {
            throw new IllegalArgumentException("Você não tem permissão para analisar pedidos");
        }

        Solicitacao pedido = buscarComAcesso(id);

        if (pedido.getStatus() != StatusSolicitacao.PENDENTE) {
            String quem = pedido.getAnalisadoPor() != null ? pedido.getAnalisadoPor().getNome() : "outra pessoa";
            String quando = pedido.getAnalisadoEm() != null
                    ? " em " + DATA_HORA_BR.format(pedido.getAnalisadoEm().atZone(FUSO))
                    : "";
            throw new IllegalStateException("Este pedido já foi "
                    + (pedido.getStatus() == StatusSolicitacao.APROVADA ? "aprovado" : "recusado")
                    + " por " + quem + quando);
        }
        if (dto.decisao() != StatusSolicitacao.APROVADA && dto.decisao() != StatusSolicitacao.RECUSADA) {
            throw new IllegalArgumentException("Decisão inválida: use APROVADA ou RECUSADA");
        }
        if (pedido.getUsuario().getId().equals(analista.getId())) {
            throw new IllegalArgumentException("Você não pode analisar um pedido seu — outra pessoa precisa fazer isso");
        }

        // Aprovar mexe no mês; recusar não. Por isso só a aprovação é barrada
        // num mês fechado (o pedido pendente ainda pode ser recusado).
        if (dto.decisao() == StatusSolicitacao.APROVADA) {
            fechamentoService.exigirAberto(pedido.getUsuario().getEmpresa().getId(), pedido.getData(),
                    "Não é possível aprovar");
        }

        Instant agora = Instant.now();
        pedido.setStatus(dto.decisao());
        pedido.setParecer(dto.parecer().trim());
        pedido.setAnalisadoPor(analista);
        pedido.setAnalisadoEm(agora);

        if (dto.decisao() == StatusSolicitacao.APROVADA && pedido.getTipo() == TipoSolicitacao.AJUSTE) {
            pedido.setMarcacaoGerada(criarMarcacaoDeAjuste(pedido, analista, agora));
        }

        return solicitacaoRepository.save(pedido);
    }

    /**
     * Marcação nova do ajuste aprovado. Não tem aparelho nem desvio de
     * relógio: a hora é exatamente a pretendida no pedido. Quem "registrou" é
     * quem aprovou. A original nunca é tocada.
     */
    private Marcacao criarMarcacaoDeAjuste(Solicitacao pedido, Usuario analista, Instant agora) {
        Usuario funcionario = pedido.getUsuario();
        LocalDate data = pedido.getData();

        // Setor: o da alocação que valia no dia, se for uma só (se houver mais
        // de uma, não dá para adivinhar — fica sem setor).
        List<Alocacao> doDia = alocacaoRepository.findAllByUsuarioId(funcionario.getId()).stream()
                .filter(a -> !data.isBefore(a.getDataInicio()) && !data.isAfter(a.getDataFim()))
                .toList();
        Setor setor = doDia.size() == 1 ? doDia.get(0).getSetor() : null;

        Marcacao marcacao = new Marcacao();
        marcacao.setId(UUID.randomUUID());
        marcacao.setUsuario(funcionario);
        marcacao.setRegistradoPor(analista);
        marcacao.setSetor(setor);
        marcacao.setTipo(pedido.getTipoMarcacao());
        marcacao.setHoraAparelho(LocalDateTime.of(data, pedido.getHoraPretendida()).atZone(FUSO).toInstant());
        marcacao.setDesvioRelogioMs(null);
        marcacao.setOffline(false);
        marcacao.setRecebidaEm(agora);
        marcacao.setStatus(StatusMarcacao.VALIDA);
        marcacao.setSolicitacao(pedido);
        return marcacaoRepository.save(marcacao);
    }

    // ------------------------------------------------------------- consultas

    // Fila do gestor/RH. Sempre da empresa de quem pede (superadmin escolhe).
    public PaginaDTO<SolicitacaoResponseDTO> fila(SolicitacaoFiltroDTO filtro, int pagina, int tamanho) {
        UUID empresaId = TenantContext.isSuperAdmin() ? filtro.empresaId() : TenantContext.getEmpresaId();
        if (!TenantContext.isSuperAdmin() && empresaId == null) {
            throw new IllegalArgumentException("Sessão sem empresa");
        }

        Specification<Solicitacao> spec = Specification
                .where(SolicitacaoSpecification.comEmpresaId(empresaId))
                .and(SolicitacaoSpecification.comUsuarioNome(filtro.usuarioNome()))
                .and(SolicitacaoSpecification.comStatus(filtro.status()))
                .and(SolicitacaoSpecification.comTipo(filtro.tipo()))
                .and(SolicitacaoSpecification.comDataEntre(filtro.dataDe(), filtro.dataAte()))
                .and(SolicitacaoSpecification.ordemDaFila());

        // Sem "sort" no Pageable: quem manda na ordem é ordemDaFila().
        Pageable pageable = PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamanho), 100));
        Page<SolicitacaoResponseDTO> resultado = solicitacaoRepository.findAll(spec, pageable)
                .map(SolicitacaoResponseDTO::fromEntity);
        return PaginaDTO.fromPage(resultado);
    }

    // Pedidos do próprio usuário logado (para o app).
    public List<Solicitacao> meus() {
        return solicitacaoRepository.findAllByUsuarioIdOrderByCriadoEmDesc(TenantContext.getUsuarioId());
    }

    public Solicitacao buscar(UUID id) {
        return buscarComAcesso(id);
    }

    @Transactional(readOnly = true)
    public SolicitacaoContextoDTO contexto(UUID id) {
        Solicitacao pedido = buscarComAcesso(id);
        Usuario funcionario = pedido.getUsuario();
        LocalDate data = pedido.getData();

        EspelhoDiaDTO dia = espelhoService.agendaDoPeriodo(funcionario, data, data).get(0);
        List<MarcacaoResponseDTO> marcacoes = espelhoService.marcacoesDoPeriodo(funcionario.getId(), data, data)
                .stream()
                .map(MarcacaoResponseDTO::fromEntity)
                .toList();

        return new SolicitacaoContextoDTO(
                dia.situacao(),
                dia.descricao(),
                dia.turnos(),
                marcacoes,
                fechamentoService.estaFechado(funcionario.getEmpresa().getId(), data)
        );
    }

    public AfastamentoService.AnexoDownload baixarAnexo(UUID id) {
        Solicitacao pedido = buscarComAcesso(id);
        if (pedido.getAnexoArquivo() == null) {
            throw new IllegalArgumentException("Este pedido não tem anexo");
        }
        return new AfastamentoService.AnexoDownload(
                pedido.getAnexoNomeOriginal(),
                pedido.getAnexoTipoConteudo(),
                anexoStorageService.ler(pedido.getAnexoArquivo(), AnexoStorageService.SOLICITACOES)
        );
    }

    // ------------------------------------------------------------- auxiliares

    private Usuario resolverFuncionario(UUID usuarioId, Usuario logado) {
        Usuario funcionario;
        if (usuarioId == null || usuarioId.equals(logado.getId())) {
            funcionario = logado;
        } else {
            if (!ANALISTAS.contains(logado.getPerfil())) {
                throw new IllegalArgumentException("Você só pode fazer pedidos para você mesmo");
            }
            funcionario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));
            if (!mesmaEmpresa(funcionario)) {
                throw new IllegalArgumentException("Funcionário não encontrado");
            }
        }

        if (funcionario.getEmpresa() == null
                || funcionario.getPerfil() == Perfil.SUPERADMIN
                || funcionario.getPerfil() == Perfil.USUARIO_SETOR) {
            throw new IllegalArgumentException("Este usuário não bate ponto, então não pode ter pedido de ajuste ou justificativa");
        }
        return funcionario;
    }

    private void validarSemPedidoRepetido(Solicitacao pedido) {
        UUID usuarioId = pedido.getUsuario().getId();
        boolean repetido = pedido.getTipo() == TipoSolicitacao.AJUSTE
                ? solicitacaoRepository.existsByUsuarioIdAndTipoAndDataAndTipoMarcacaoAndStatus(
                        usuarioId, TipoSolicitacao.AJUSTE, pedido.getData(), pedido.getTipoMarcacao(), StatusSolicitacao.PENDENTE)
                : solicitacaoRepository.existsByUsuarioIdAndTipoAndDataAndOcorrenciaAndStatus(
                        usuarioId, TipoSolicitacao.JUSTIFICATIVA, pedido.getData(), pedido.getOcorrencia(), StatusSolicitacao.PENDENTE);

        if (repetido) {
            throw new IllegalStateException("Já existe um pedido igual pendente para "
                    + pedido.getData().format(DATA_BR) + " — aguarde a análise dele");
        }
    }

    // Funcionário vê só os próprios pedidos; gestor/RH vê os da empresa.
    private Solicitacao buscarComAcesso(UUID id) {
        Solicitacao pedido = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado"));

        boolean ehDono = pedido.getUsuario().getId().equals(TenantContext.getUsuarioId());
        boolean ehAnalistaDaEmpresa = ANALISTAS.contains(TenantContext.getPerfil()) && mesmaEmpresa(pedido.getUsuario());
        if (!ehDono && !ehAnalistaDaEmpresa) {
            throw new IllegalArgumentException("Pedido não encontrado");
        }
        return pedido;
    }

    private boolean mesmaEmpresa(Usuario usuario) {
        if (TenantContext.isSuperAdmin()) {
            return true;
        }
        UUID empresaId = TenantContext.getEmpresaId();
        return empresaId != null && usuario.getEmpresa() != null && usuario.getEmpresa().getId().equals(empresaId);
    }

    private Usuario usuarioLogado() {
        return usuarioRepository.findById(TenantContext.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Sessão inválida"));
    }
}
