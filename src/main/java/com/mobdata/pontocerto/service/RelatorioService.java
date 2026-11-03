package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AgrupamentoRelatorio;
import com.mobdata.pontocerto.dto.EspelhoDiaDTO;
import com.mobdata.pontocerto.dto.EspelhoTurnoDTO;
import com.mobdata.pontocerto.dto.RelatorioIndicadoresDTO;
import com.mobdata.pontocerto.dto.RelatorioLinhaDTO;
import com.mobdata.pontocerto.dto.RelatorioPeriodoDTO;
import com.mobdata.pontocerto.dto.RelatorioPrevistoRealizadoDTO;
import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.SetorRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * W12 — Relatório de previsto contra realizado.
 *
 * Para cada funcionário da empresa, pega os dias do espelho no período
 * (mesma conta da tela de espelho) e soma por funcionário, por setor e no
 * total, quebrando em semanas ou meses.
 *
 * Isolamento: a empresa vem SEMPRE do token (só o superadmin escolhe via
 * parâmetro). Setor e funcionário filtrados são conferidos contra essa
 * empresa — id de outra empresa responde "não encontrado".
 *
 * Setor de cada dia: o setor do primeiro turno previsto no dia; se o dia não
 * tem turno (folga, feriado, afastamento), o setor da alocação que valia na
 * data. Assim quem trocou de setor no meio do mês tem cada dia contado no
 * setor certo.
 */
@Service
public class RelatorioService {

    private static final ZoneId FUSO = ZoneId.of("America/Recife");
    private static final int MAXIMO_DIAS = 366;
    private static final String SEM_SETOR = "Sem setor";
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SetorRepository setorRepository;

    @Autowired
    private AlocacaoRepository alocacaoRepository;

    @Autowired
    private EspelhoService espelhoService;

    private record SetorDoDia(UUID id, String nome) {
    }

    @Transactional(readOnly = true)
    public RelatorioPrevistoRealizadoDTO previstoRealizado(LocalDate inicio, LocalDate fim,
                                                          AgrupamentoRelatorio agrupamento,
                                                          UUID setorId, UUID usuarioId,
                                                          UUID empresaIdParam) {
        UUID empresaId = empresaDaRequisicao(empresaIdParam);
        LocalDate hoje = LocalDate.now(FUSO);
        AgrupamentoRelatorio agrupamentoFinal = agrupamento == null ? AgrupamentoRelatorio.MES : agrupamento;

        validarPeriodo(inicio, fim, hoje);
        LocalDate calculadoAte = fim.isAfter(hoje) ? hoje : fim;

        if (setorId != null) {
            Setor setor = setorRepository.findById(setorId)
                    .orElseThrow(() -> new IllegalArgumentException("Setor não encontrado"));
            if (!setor.getEmpresa().getId().equals(empresaId)) {
                throw new IllegalArgumentException("Setor não encontrado");
            }
        }

        List<Usuario> funcionarios = funcionariosDoRelatorio(empresaId, usuarioId);
        List<RelatorioPeriodoDTO> periodos = RelatorioCalculo.montarPeriodos(inicio, calculadoAte, agrupamentoFinal);

        List<RelatorioCalculo.Linha> linhasFuncionarios = new ArrayList<>();
        Map<UUID, RelatorioCalculo.Linha> linhasSetores = new LinkedHashMap<>();
        RelatorioCalculo.Linha total = new RelatorioCalculo.Linha(null, "Total", null, periodos);

        for (Usuario funcionario : funcionarios) {
            List<Alocacao> alocacoes = alocacaoRepository.findAllByUsuarioId(funcionario.getId());
            List<EspelhoDiaDTO> dias = espelhoService.diasDoPeriodo(funcionario, inicio, calculadoAte);

            RelatorioCalculo.Linha linha = new RelatorioCalculo.Linha(
                    funcionario.getId(), funcionario.getNome(), funcionario.getMatricula(), periodos);

            for (EspelhoDiaDTO dia : dias) {
                if (!RelatorioCalculo.diaRelevante(dia)) {
                    continue;
                }
                SetorDoDia setorDoDia = setorDoDia(dia, alocacoes);
                if (setorId != null && (setorDoDia.id() == null || !setorId.equals(setorDoDia.id()))) {
                    continue; // filtro por setor: só os dias trabalhados naquele setor
                }

                linha.somarDia(dia, hoje);
                total.somarDia(dia, hoje);
                linhasSetores
                        .computeIfAbsent(setorDoDia.id(),
                                id -> new RelatorioCalculo.Linha(id, setorDoDia.nome(), null, periodos))
                        .somarDia(dia, hoje);
            }

            if (!linha.vazia()) {
                linhasFuncionarios.add(linha);
            }
        }

        List<RelatorioLinhaDTO> funcionariosDTO = linhasFuncionarios.stream()
                .sorted(Comparator.comparing(RelatorioCalculo.Linha::nome, String.CASE_INSENSITIVE_ORDER))
                .map(RelatorioCalculo.Linha::paraDTO)
                .toList();

        // Setores em ordem alfabética, "Sem setor" sempre por último.
        List<RelatorioLinhaDTO> setoresDTO = linhasSetores.values().stream()
                .sorted(Comparator.comparing((RelatorioCalculo.Linha l) -> l.id() == null)
                        .thenComparing(RelatorioCalculo.Linha::nome, String.CASE_INSENSITIVE_ORDER))
                .map(RelatorioCalculo.Linha::paraDTO)
                .toList();

        return new RelatorioPrevistoRealizadoDTO(
                inicio, fim, calculadoAte, agrupamentoFinal, periodos,
                funcionariosDTO, setoresDTO, total.paraDTO());
    }

    /**
     * Mesmo relatório em CSV (separador ";" e BOM UTF-8, que é o que o Excel
     * em português abre direto, com acento certo). Uma linha por
     * funcionário/setor e período, mais a linha "Total do período" de cada um.
     */
    @Transactional(readOnly = true)
    public byte[] previstoRealizadoCsv(LocalDate inicio, LocalDate fim, AgrupamentoRelatorio agrupamento,
                                       UUID setorId, UUID usuarioId, UUID empresaIdParam) {
        RelatorioPrevistoRealizadoDTO relatorio =
                previstoRealizado(inicio, fim, agrupamento, setorId, usuarioId, empresaIdParam);

        StringBuilder csv = new StringBuilder("﻿");
        linhaCsv(csv, "Relatório", "Previsto contra realizado");
        linhaCsv(csv, "Período", DATA_BR.format(relatorio.inicio()) + " a " + DATA_BR.format(relatorio.fim()));
        linhaCsv(csv, "Calculado até", DATA_BR.format(relatorio.calculadoAte()));
        csv.append("\r\n");

        linhaCsv(csv,
                "Tipo", "Nome", "Matrícula", "Período",
                "Previsto (h)", "Realizado (h)", "Diferença (h)", "Horas extras (h)",
                "Atrasos justificados", "Atrasos não justificados", "Minutos de atraso",
                "Faltas justificadas", "Faltas não justificadas",
                "Dias afastado", "Afastamentos", "Dias em aberto");

        for (RelatorioLinhaDTO linha : relatorio.funcionarios()) {
            escreverLinhas(csv, "Funcionário", linha, relatorio.periodos());
        }
        for (RelatorioLinhaDTO linha : relatorio.setores()) {
            escreverLinhas(csv, "Setor", linha, relatorio.periodos());
        }
        escreverLinhas(csv, "Total geral", relatorio.totalGeral(), relatorio.periodos());

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------------

    private void escreverLinhas(StringBuilder csv, String tipo, RelatorioLinhaDTO linha,
                                List<RelatorioPeriodoDTO> periodos) {
        // Com um período só, a linha do período já é o total: não repete.
        if (periodos.size() > 1) {
            for (int i = 0; i < periodos.size(); i++) {
                escreverIndicadores(csv, tipo, linha, periodos.get(i).rotulo(), linha.porPeriodo().get(i));
            }
        }
        String rotuloTotal = periodos.size() == 1 ? periodos.get(0).rotulo() : "Total do período";
        escreverIndicadores(csv, tipo, linha, rotuloTotal, linha.total());
    }

    private void escreverIndicadores(StringBuilder csv, String tipo, RelatorioLinhaDTO linha,
                                     String periodo, RelatorioIndicadoresDTO i) {
        linhaCsv(csv,
                tipo,
                linha.nome(),
                linha.detalhe() == null ? "" : linha.detalhe(),
                periodo,
                horas(i.minutosPrevistos()),
                horas(i.minutosRealizados()),
                horas(i.diferencaMinutos()),
                horas(i.minutosExtras()),
                String.valueOf(i.atrasosJustificados()),
                String.valueOf(i.atrasosNaoJustificados()),
                String.valueOf(i.minutosAtraso()),
                String.valueOf(i.faltasJustificadas()),
                String.valueOf(i.faltasNaoJustificadas()),
                String.valueOf(i.diasAfastamento()),
                String.join(", ", i.afastamentos()),
                String.valueOf(i.diasEmAberto()));
    }

    // 125 -> "2:05"; -90 -> "-1:30"
    static String horas(long minutos) {
        long abs = Math.abs(minutos);
        return (minutos < 0 ? "-" : "") + (abs / 60) + ":" + String.format("%02d", abs % 60);
    }

    private static void linhaCsv(StringBuilder csv, String... campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) csv.append(';');
            csv.append(escaparCsv(campos[i]));
        }
        csv.append("\r\n");
    }

    private static String escaparCsv(String valor) {
        if (valor == null) return "";
        // Evita fórmula do Excel em nome digitado por usuário (=, +, -, @).
        String seguro = valor;
        if (!seguro.isEmpty() && "=+@".indexOf(seguro.charAt(0)) >= 0) {
            seguro = "'" + seguro;
        }
        if (seguro.contains(";") || seguro.contains("\"") || seguro.contains("\n") || seguro.contains("\r")) {
            return "\"" + seguro.replace("\"", "\"\"") + "\"";
        }
        return seguro;
    }

    private SetorDoDia setorDoDia(EspelhoDiaDTO dia, List<Alocacao> alocacoes) {
        if (!dia.turnos().isEmpty()) {
            EspelhoTurnoDTO primeiro = dia.turnos().get(0);
            return new SetorDoDia(primeiro.setorId(), primeiro.setorNome());
        }
        LocalDate data = dia.data();
        return alocacoes.stream()
                .filter(a -> !data.isBefore(a.getDataInicio()) && !data.isAfter(a.getDataFim()))
                .min(Comparator.comparing(Alocacao::getDataInicio))
                .map(a -> new SetorDoDia(a.getSetor().getId(), a.getSetor().getNome()))
                .orElse(new SetorDoDia(null, SEM_SETOR));
    }

    private List<Usuario> funcionariosDoRelatorio(UUID empresaId, UUID usuarioId) {
        if (usuarioId == null) {
            return usuarioRepository.findAllByEmpresaIdAndPerfil(empresaId, Perfil.FUNCIONARIO);
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));
        if (usuario.getEmpresa() == null || !usuario.getEmpresa().getId().equals(empresaId)) {
            throw new IllegalArgumentException("Funcionário não encontrado");
        }
        return List.of(usuario);
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim, LocalDate hoje) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Informe o início e o fim do período");
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("O fim do período não pode ser antes do início");
        }
        if (inicio.isAfter(hoje)) {
            throw new IllegalArgumentException("O período ainda não começou — escolha datas até hoje");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > MAXIMO_DIAS) {
            throw new IllegalArgumentException("O período pode ter no máximo " + MAXIMO_DIAS + " dias");
        }
    }

    private UUID empresaDaRequisicao(UUID empresaIdParam) {
        UUID empresaId = TenantContext.isSuperAdmin() ? empresaIdParam : TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new IllegalArgumentException("Informe a empresa (empresaId)");
        }
        return empresaId;
    }
}
