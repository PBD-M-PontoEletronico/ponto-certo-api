package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AlocacaoRequestDTO;
import com.mobdata.pontocerto.dto.AlocacaoResponseDTO;
import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    public Alocacao alocar(AlocacaoRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        Setor setor = setorService.buscarPorId(request.setorId()); // já valida empresa

        if (usuario.getEmpresa() == null || !usuario.getEmpresa().getId().equals(setor.getEmpresa().getId())) {
            throw new IllegalArgumentException("Funcionário e setor pertencem a empresas diferentes");
        }

        if (request.dataFim() != null && request.dataFim().isBefore(request.dataInicio())) {
            throw new IllegalArgumentException("Data de fim não pode ser anterior à data de início");
        }

        List<Alocacao> sobreposicoes = alocacaoRepository.buscarSobreposicoes(
                usuario.getId(), setor.getId(),request.dataInicio(), request.dataFim()
        );

        if (!sobreposicoes.isEmpty()) {
            Alocacao conflito = sobreposicoes.get(0);
            throw new IllegalStateException(
                    "Conflito de alocação: \"" + usuario.getNome() + "\" já está alocado no setor \""
                            + conflito.getSetor().getNome() + "\" entre " + conflito.getDataInicio()
                            + " e " + (conflito.getDataFim() != null ? conflito.getDataFim() : "o momento atual")
            );
        }

        Alocacao alocacao = new Alocacao();
        alocacao.setUsuario(usuario);
        alocacao.setSetor(setor);
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
                .findAllBySetorIdAndDataFimIsNull(setorId)
                .stream()
                .map(AlocacaoResponseDTO::fromEntity)
                .toList();
    }
}