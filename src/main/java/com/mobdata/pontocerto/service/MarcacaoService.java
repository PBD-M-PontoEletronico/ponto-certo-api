package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.*;
import com.mobdata.pontocerto.exception.MarcacaoRecusadaException;
import com.mobdata.pontocerto.model.*;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.DispositivoRepository;
import com.mobdata.pontocerto.repository.MarcacaoRepository;
import com.mobdata.pontocerto.repository.SetorRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import com.mobdata.pontocerto.specification.MarcacaoSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class MarcacaoService {

    private static final ZoneId FUSO = ZoneId.of("America/Recife");

    @Autowired
    private MarcacaoRepository marcacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SetorRepository setorRepository;

    @Autowired
    private DispositivoRepository dispositivoRepository;

    @Autowired
    private AlocacaoRepository alocacaoRepository;

    @Autowired
    private SelfieStorageService selfieStorageService;

    @Transactional
    public MarcacaoResponseDTO registrar(MarcacaoForm form) {
        // Idempotência: o id vem do aparelho. Reenvio de sincronização offline
        // cai aqui de novo com o mesmo id — devolve o que já existe, sem
        // reprocessar (e sem risco de duplicar a marcação).
        var existente = marcacaoRepository.findById(form.getId());
        if (existente.isPresent()) {
            if (!existente.get().getUsuario().getId().equals(form.getUsuarioId())) {
                throw new IllegalStateException("Conflito de identificador de marcação");
            }
            return MarcacaoResponseDTO.fromEntity(existente.get());
        }

        Usuario usuario = usuarioRepository.findById(form.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        UUID registradoPorId = TenantContext.getUsuarioId();
        Usuario registradoPor = registradoPorId.equals(usuario.getId())
                ? usuario
                : usuarioRepository.findById(registradoPorId)
                .orElseThrow(() -> new IllegalArgumentException("Sessão inválida"));

        // No celular pessoal (perfil != USUARIO_SETOR), só é permitido bater o próprio ponto.
        if (registradoPor.getPerfil() != Perfil.USUARIO_SETOR && !registradoPor.getId().equals(usuario.getId())) {
            throw new MarcacaoRecusadaException("Não é possível registrar marcação de outro usuário por este aparelho");
        }

        // 1) Dispositivo ativo, vinculado a quem está logado no aparelho.
        Dispositivo dispositivo = dispositivoRepository.findByUsuarioIdAndAtivoTrue(registradoPor.getId())
                .filter(d -> d.getIdentificador().equals(form.getIdentificadorDispositivo()))
                .orElseThrow(() -> new MarcacaoRecusadaException("Dispositivo não registrado ou não é mais o ativo"));

        // Relógio de setor: o setor é sempre o da própria conta — preenche
        // automaticamente se o aparelho não mandou, recusa se mandou outro.
        UUID setorId = form.getSetorId();
        if (registradoPor.getPerfil() == Perfil.USUARIO_SETOR) {
            if (registradoPor.getSetor() == null) {
                throw new MarcacaoRecusadaException("Esta conta de setor não está vinculada a nenhum setor");
            }
            if (setorId != null && !setorId.equals(registradoPor.getSetor().getId())) {
                throw new MarcacaoRecusadaException("Este relógio só registra marcações do próprio setor");
            }
            setorId = registradoPor.getSetor().getId();
        }

        Setor setor = setorId != null ? setorRepository.findById(setorId).orElse(null) : null;

        Marcacao marcacao = new Marcacao();
        marcacao.setId(form.getId());
        marcacao.setUsuario(usuario);
        marcacao.setRegistradoPor(registradoPor);
        marcacao.setSetor(setor);
        marcacao.setDispositivo(dispositivo);
        marcacao.setTipo(form.getTipo());
        marcacao.setHoraAparelho(Instant.ofEpochMilli(form.getHoraAparelhoEpochMs()));
        marcacao.setDesvioRelogioMs(form.getDesvioRelogioMs());
        marcacao.setOffline(form.isOffline());
        marcacao.setRecebidaEm(Instant.now());
        marcacao.setLatitude(form.getLatitude());
        marcacao.setLongitude(form.getLongitude());
        marcacao.setMetodoConfirmacao(form.getMetodoConfirmacao());
        marcacao.setStatus(StatusMarcacao.VALIDA);

        if (setor == null) {
            // Aparelho não soube informar o setor — não dá pra validar
            // perímetro, selfie exigida nem alocação; manda pra análise do RH.
            marcacao.setStatus(StatusMarcacao.PENDENTE_ANALISE);
            marcacao.setMotivoRecusa("Setor não informado pelo aparelho");
        } else {
            // 3) Localização
            if (!setor.isIgnorarLocalizacao() && form.getLatitude() != null && form.getLongitude() != null) {
                double distancia = calcularDistanciaMetros(
                        setor.getLatitude(), setor.getLongitude(), form.getLatitude(), form.getLongitude());
                if (distancia > setor.getRaioMetros()) {
                    if (setor.getPoliticaForaPerimetro() == PoliticaForaPerimetro.BLOQUEAR) {
                        marcacao.setStatus(StatusMarcacao.RECUSADA);
                        marcacao.setMotivoRecusa("Fora do perímetro do setor (" + Math.round(distancia) + "m do permitido)");
                    } else {
                        marcacao.setStatus(StatusMarcacao.PENDENTE_ANALISE);
                        marcacao.setMotivoRecusa("Fora do perímetro, aguardando análise (" + Math.round(distancia) + "m)");
                    }
                }
            }

            // 4) Selfie/biometria — a confirmação em si já aconteceu no
            // aparelho (ver MarcacaoForm); aqui só confere que a evidência
            // exigida pela política do setor veio junto.
            if (marcacao.getStatus() == StatusMarcacao.VALIDA) {
                if (setor.isExigirSelfie() && form.getMetodoConfirmacao() != MetodoConfirmacao.FOTO) {
                    marcacao.setStatus(StatusMarcacao.RECUSADA);
                    marcacao.setMotivoRecusa("Este setor exige confirmação por foto");
                } else if (form.getMetodoConfirmacao() == MetodoConfirmacao.FOTO
                        && (form.getFoto() == null || form.getFoto().isEmpty())) {
                    marcacao.setStatus(StatusMarcacao.RECUSADA);
                    marcacao.setMotivoRecusa("Foto obrigatória não enviada");
                } else if (form.getMetodoConfirmacao() == MetodoConfirmacao.DIGITAL
                        && !Boolean.TRUE.equals(form.getDigitalConfirmada())) {
                    marcacao.setStatus(StatusMarcacao.RECUSADA);
                    marcacao.setMotivoRecusa("Confirmação biométrica não realizada");
                }
            }

            // 5) Alocação: usuário precisa estar alocado neste setor hoje.
            if (marcacao.getStatus() == StatusMarcacao.VALIDA) {
                LocalDate hoje = LocalDate.now(FUSO);
                UUID setorIdFinal = setor.getId();
                boolean alocado = alocacaoRepository.findAllByUsuarioId(usuario.getId()).stream()
                        .anyMatch(a -> a.getSetor().getId().equals(setorIdFinal)
                                && !a.getDataInicio().isAfter(hoje)
                                && !a.getDataFim().isBefore(hoje));
                if (!alocado) {
                    marcacao.setStatus(StatusMarcacao.RECUSADA);
                    marcacao.setMotivoRecusa("Usuário não está alocado neste setor hoje");
                }
            }
        }

        // A foto é guardada mesmo quando a marcação acaba recusada/pendente —
        // é evidência útil pro RH revisar depois.
        String selfieArquivoSalvo = null;
        MultipartFile foto = form.getFoto();
        if (form.getMetodoConfirmacao() == MetodoConfirmacao.FOTO && foto != null && !foto.isEmpty()) {
            SelfieStorageService.SelfieSalva selfieSalva = selfieStorageService.salvar(foto);
            selfieArquivoSalvo = selfieSalva.arquivo();
            marcacao.setSelfieArquivo(selfieSalva.arquivo());
            marcacao.setSelfieTipoConteudo(selfieSalva.tipoConteudo());
        }

        try {
            return MarcacaoResponseDTO.fromEntity(marcacaoRepository.save(marcacao));
        } catch (RuntimeException e) {
            if (selfieArquivoSalvo != null) {
                selfieStorageService.remover(selfieArquivoSalvo);
            }
            throw e;
        }
    }

    public PaginaDTO<MarcacaoResponseDTO> buscar(MarcacaoFiltroDTO filtro, Pageable pageable) {
        UUID empresaId = TenantContext.isSuperAdmin() ? filtro.empresaId() : TenantContext.getEmpresaId();

        Specification<Marcacao> spec = Specification
                .where(MarcacaoSpecification.comUsuarioNome(filtro.usuarioNome()))
                .and(MarcacaoSpecification.comSetorId(filtro.setorId()))
                .and(MarcacaoSpecification.comEmpresaId(empresaId))
                .and(MarcacaoSpecification.comStatus(filtro.status()))
                .and(MarcacaoSpecification.comTipo(filtro.tipo()))
                .and(MarcacaoSpecification.comDataEntre(filtro.dataDe(), filtro.dataAte()));

        Page<MarcacaoResponseDTO> pagina = marcacaoRepository.findAll(spec, pageable)
                .map(MarcacaoResponseDTO::fromEntity);

        return PaginaDTO.fromPage(pagina);
    }

    @Transactional
    public MarcacaoResponseDTO revisar(UUID marcacaoId, RevisarMarcacaoDTO dto) {
        Marcacao marcacao = marcacaoRepository.findById(marcacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Marcação não encontrada"));

        verificarAcesso(marcacao);

        if (marcacao.getStatus() != StatusMarcacao.PENDENTE_ANALISE) {
            throw new IllegalStateException("Só é possível revisar marcações pendentes de análise");
        }
        if (dto.novoStatus() != StatusMarcacao.VALIDA && dto.novoStatus() != StatusMarcacao.RECUSADA) {
            throw new IllegalArgumentException("Status de revisão inválido");
        }

        marcacao.setStatus(dto.novoStatus());
        marcacao.setMotivoRecusa(dto.novoStatus() == StatusMarcacao.RECUSADA ? dto.motivo() : null);

        return MarcacaoResponseDTO.fromEntity(marcacaoRepository.save(marcacao));
    }

    public SelfieDTO obterSelfie(UUID marcacaoId) {
        Marcacao marcacao = marcacaoRepository.findById(marcacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Marcação não encontrada"));

        verificarAcesso(marcacao);

        if (marcacao.getSelfieArquivo() == null) {
            throw new IllegalArgumentException("Esta marcação não tem foto");
        }
        return new SelfieDTO(
                selfieStorageService.ler(marcacao.getSelfieArquivo()),
                marcacao.getSelfieTipoConteudo()
        );
    }

    private double calcularDistanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        final double raioTerraMetros = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return raioTerraMetros * c;
    }

    private void verificarAcesso(Marcacao marcacao) {
        if (TenantContext.isSuperAdmin()) {
            return;
        }
        UUID empresaId = TenantContext.getEmpresaId();
        UUID empresaDaMarcacao = marcacao.getUsuario().getEmpresa() != null
                ? marcacao.getUsuario().getEmpresa().getId()
                : null;
        if (empresaId == null || !empresaId.equals(empresaDaMarcacao)) {
            throw new IllegalArgumentException("Marcação não encontrada");
        }
    }
}