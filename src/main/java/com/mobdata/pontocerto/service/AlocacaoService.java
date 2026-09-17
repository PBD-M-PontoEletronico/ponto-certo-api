package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AgendaTurnoDTO;
import com.mobdata.pontocerto.dto.AlocacaoRequestDTO;
import com.mobdata.pontocerto.dto.AlocacaoResponseDTO;
import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.model.Escala;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AlocacaoService {

    @Autowired
    private AlocacaoRepository alocacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SetorService setorService; // reaproveita a checagem de acesso já existente

    @Autowired
    private EscalaService escalaService; // idem, para escala

    public Alocacao alocar(AlocacaoRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        Setor setor = setorService.buscarPorId(request.setorId()); // já valida empresa
        Escala escala = escalaService.buscarPorId(request.escalaId()); // já valida empresa

        if (usuario.getEmpresa() == null || !usuario.getEmpresa().getId().equals(setor.getEmpresa().getId())) {
            throw new IllegalArgumentException("Funcionário e setor pertencem a empresas diferentes");
        }

        if (!setor.getEmpresa().getId().equals(escala.getEmpresa().getId())) {
            throw new IllegalArgumentException("Setor e escala pertencem a empresas diferentes");
        }

        if (request.dataFim().isBefore(request.dataInicio())) {
            throw new IllegalArgumentException("Data de fim não pode ser anterior à data de início");
        }

        validarSemConflito(usuario, escala, request.dataInicio(), request.dataFim());

        Alocacao alocacao = new Alocacao();
        alocacao.setUsuario(usuario);
        alocacao.setSetor(setor);
        alocacao.setEscala(escala);
        alocacao.setDataInicio(request.dataInicio());
        alocacao.setDataFim(request.dataFim());

        return alocacaoRepository.save(alocacao);
    }

    public Alocacao encerrar(UUID alocacaoId, LocalDate dataFim) {
        Alocacao alocacao = alocacaoRepository.findById(alocacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Alocação não encontrada"));

        setorService.buscarPorId(alocacao.getSetor().getId()); // garante acesso

        alocacao.setDataFim(dataFim);
        return alocacaoRepository.save(alocacao);
    }

    public List<AlocacaoResponseDTO> historicoDoFuncionario(UUID usuarioId) {
        return alocacaoRepository
                .findAllByUsuarioId(usuarioId)
                .stream()
                .map(AlocacaoResponseDTO::fromEntity)
                .toList();
    }

    public List<AlocacaoResponseDTO> alocadosAtualmenteNoSetor(UUID setorId) {
        setorService.buscarPorId(setorId);
        return alocacaoRepository
                .findAllBySetorIdAndDataFimGreaterThanEqual(setorId, LocalDate.now())
                .stream()
                .map(AlocacaoResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Agenda do funcionário no período: os turnos previstos, de todas as
     * alocações (em qualquer setor/escala) que tocam o intervalo pedido.
     */
    public List<AgendaTurnoDTO> agenda(UUID usuarioId, LocalDate dataInicio, LocalDate dataFim) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));
        verificarAcessoAoUsuario(usuario);

        List<AgendaTurnoDTO> agenda = new ArrayList<>();

        for (Alocacao alocacao : alocacaoRepository.findAllByUsuarioId(usuarioId)) {
            LocalDate inicio = maior(dataInicio, alocacao.getDataInicio());
            LocalDate fim = menor(dataFim, alocacao.getDataFim());
            if (inicio.isAfter(fim)) {
                continue;
            }

            for (EscalaCalculo.PeriodoTurno periodo : EscalaCalculo.projetarTurnos(alocacao.getEscala(), inicio, fim)) {
                agenda.add(new AgendaTurnoDTO(
                        periodo.inicio().toLocalDate(),
                        periodo.inicio().toLocalTime(),
                        periodo.fim().toLocalTime(),
                        !periodo.fim().toLocalDate().equals(periodo.inicio().toLocalDate()),
                        alocacao.getSetor().getId(),
                        alocacao.getSetor().getNome(),
                        alocacao.getEscala().getId(),
                        alocacao.getEscala().getNome()
                ));
            }
        }

        agenda.sort(Comparator.comparing(AgendaTurnoDTO::data).thenComparing(AgendaTurnoDTO::horaInicio));
        return agenda;
    }

    /**
     * Projeta os turnos da escala candidata no período e compara com os
     * turnos de todas as demais alocações do funcionário (em qualquer
     * setor), inclusive os que atravessam a meia-noite. Sobreposição de
     * horário é recusada; encostar (um termina exatamente quando o outro
     * começa) é aceito.
     */
    private void validarSemConflito(Usuario usuario, Escala escalaCandidata, LocalDate dataInicio, LocalDate dataFim) {
        for (Alocacao existente : alocacaoRepository.findAllByUsuarioId(usuario.getId())) {
            LocalDate inicioComparacao = maior(dataInicio, existente.getDataInicio());
            LocalDate fimComparacao = menor(dataFim, existente.getDataFim());
            if (inicioComparacao.isAfter(fimComparacao)) {
                continue; // períodos nem se tocam, não há como conflitar
            }

            // um dia de folga antes da janela, para pegar o turno do dia
            // anterior que atravessa a meia-noite e cai dentro dela
            LocalDate inicioProjecaoCandidata = maior(dataInicio, inicioComparacao.minusDays(1));
            LocalDate inicioProjecaoExistente = maior(existente.getDataInicio(), inicioComparacao.minusDays(1));

            List<EscalaCalculo.PeriodoTurno> periodosCandidato =
                    EscalaCalculo.projetarTurnos(escalaCandidata, inicioProjecaoCandidata, fimComparacao);
            List<EscalaCalculo.PeriodoTurno> periodosExistente =
                    EscalaCalculo.projetarTurnos(existente.getEscala(), inicioProjecaoExistente, fimComparacao);

            for (EscalaCalculo.PeriodoTurno novo : periodosCandidato) {
                for (EscalaCalculo.PeriodoTurno velho : periodosExistente) {
                    if (novo.inicio().isBefore(velho.fim()) && velho.inicio().isBefore(novo.fim())) {
                        throw new IllegalStateException(
                                "Conflito de alocação: \"" + usuario.getNome() + "\" já está escalado em \""
                                        + existente.getEscala().getNome() + "\" no setor \""
                                        + existente.getSetor().getNome() + "\" no dia " + velho.inicio().toLocalDate()
                        );
                    }
                }
            }
        }
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

    private static LocalDate maior(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate menor(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
