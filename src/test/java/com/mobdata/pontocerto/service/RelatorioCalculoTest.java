package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AgrupamentoRelatorio;
import com.mobdata.pontocerto.dto.EspelhoDiaDTO;
import com.mobdata.pontocerto.dto.EspelhoJustificativaDTO;
import com.mobdata.pontocerto.dto.EspelhoMarcacaoDTO;
import com.mobdata.pontocerto.dto.RelatorioIndicadoresDTO;
import com.mobdata.pontocerto.dto.RelatorioPeriodoDTO;
import com.mobdata.pontocerto.dto.SituacaoDia;
import com.mobdata.pontocerto.model.StatusMarcacao;
import com.mobdata.pontocerto.model.StatusSolicitacao;
import com.mobdata.pontocerto.model.TipoMarcacao;
import com.mobdata.pontocerto.model.TipoOcorrencia;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioCalculoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 20); // terça

    // ---------- montagem de dias de teste ----------

    private static EspelhoMarcacaoDTO batida(StatusMarcacao status) {
        return new EspelhoMarcacaoDTO(UUID.randomUUID(), TipoMarcacao.ENTRADA, Instant.now(), status,
                false, false, null, null, null);
    }

    private static EspelhoJustificativaDTO justificativa(TipoOcorrencia ocorrencia, StatusSolicitacao status) {
        return new EspelhoJustificativaDTO(UUID.randomUUID(), ocorrencia, status, null, null, null);
    }

    private static EspelhoDiaDTO dia(LocalDate data, SituacaoDia situacao, String descricao, int previsto,
                                     Integer trabalhado, Integer atraso,
                                     List<EspelhoMarcacaoDTO> marcacoes,
                                     List<EspelhoJustificativaDTO> justificativas) {
        return new EspelhoDiaDTO(data, situacao, descricao, List.of(), marcacoes, justificativas,
                previsto, trabalhado, atraso, null, 0);
    }

    private static EspelhoDiaDTO trabalhou(LocalDate data, int previsto, int trabalhado, Integer atraso) {
        return dia(data, SituacaoDia.TRABALHO, null, previsto, trabalhado, atraso,
                List.of(batida(StatusMarcacao.VALIDA)), List.of());
    }

    private static EspelhoDiaDTO faltou(LocalDate data, List<EspelhoJustificativaDTO> justificativas) {
        return dia(data, SituacaoDia.TRABALHO, null, 480, 0, null, List.of(), justificativas);
    }

    private static RelatorioIndicadoresDTO somar(EspelhoDiaDTO... dias) {
        RelatorioCalculo.Acumulador acumulador = new RelatorioCalculo.Acumulador();
        for (EspelhoDiaDTO d : dias) {
            acumulador.somarDia(d, HOJE);
        }
        return acumulador.paraDTO();
    }

    // ---------- previsto x realizado ----------

    @Test
    void diferencaEhRealizadoMenosPrevisto() {
        var r = somar(
                trabalhou(LocalDate.of(2026, 10, 13), 480, 500, null),
                trabalhou(LocalDate.of(2026, 10, 14), 480, 420, null));

        assertThat(r.minutosPrevistos()).isEqualTo(960);
        assertThat(r.minutosRealizados()).isEqualTo(920);
        assertThat(r.diferencaMinutos()).isEqualTo(-40);
    }

    @Test
    void horasExtrasSomamSoOQuePassouDoPrevistoEmCadaDia() {
        var r = somar(
                trabalhou(LocalDate.of(2026, 10, 13), 480, 540, null), // +60
                trabalhou(LocalDate.of(2026, 10, 14), 480, 400, null)); // -80 não desconta das extras

        assertThat(r.minutosExtras()).isEqualTo(60);
    }

    @Test
    void folgaTrabalhadaContaInteiraComoExtra() {
        var folga = dia(LocalDate.of(2026, 10, 18), SituacaoDia.FOLGA, null, 0, 240, null,
                List.of(batida(StatusMarcacao.VALIDA)), List.of());

        var r = somar(folga);

        assertThat(r.minutosExtras()).isEqualTo(240);
        assertThat(r.faltasNaoJustificadas()).isZero();
    }

    // ---------- faltas ----------

    @Test
    void diaDeTrabalhoSemBatidaEhFaltaNaoJustificada() {
        var r = somar(faltou(LocalDate.of(2026, 10, 15), List.of()));

        assertThat(r.faltasNaoJustificadas()).isEqualTo(1);
        assertThat(r.faltasJustificadas()).isZero();
        assertThat(r.minutosPrevistos()).isEqualTo(480); // previsto continua: faltou gente
        assertThat(r.diferencaMinutos()).isEqualTo(-480);
    }

    @Test
    void faltaComJustificativaAprovadaEhJustificada() {
        var r = somar(faltou(LocalDate.of(2026, 10, 15),
                List.of(justificativa(TipoOcorrencia.FALTA, StatusSolicitacao.APROVADA))));

        assertThat(r.faltasJustificadas()).isEqualTo(1);
        assertThat(r.faltasNaoJustificadas()).isZero();
    }

    @Test
    void justificativaPendenteAindaNaoJustificaAFalta() {
        var r = somar(faltou(LocalDate.of(2026, 10, 15),
                List.of(justificativa(TipoOcorrencia.FALTA, StatusSolicitacao.PENDENTE))));

        assertThat(r.faltasNaoJustificadas()).isEqualTo(1);
    }

    @Test
    void justificativaDeAtrasoNaoJustificaFalta() {
        var r = somar(faltou(LocalDate.of(2026, 10, 15),
                List.of(justificativa(TipoOcorrencia.ATRASO, StatusSolicitacao.APROVADA))));

        assertThat(r.faltasNaoJustificadas()).isEqualTo(1);
    }

    @Test
    void batidaQueNaoFoiValidadaNaoEvitaAFalta() {
        var d = dia(LocalDate.of(2026, 10, 15), SituacaoDia.TRABALHO, null, 480, 0, null,
                List.of(batida(StatusMarcacao.RECUSADA)), List.of());

        assertThat(somar(d).faltasNaoJustificadas()).isEqualTo(1);
    }

    @Test
    void hojeSemBatidaNaoEhFaltaEFicaEmAberto() {
        var r = somar(faltou(HOJE, List.of()));

        assertThat(r.faltasNaoJustificadas()).isZero();
        assertThat(r.diasEmAberto()).isEqualTo(1);
        assertThat(r.minutosPrevistos()).isZero(); // não distorce a diferença
    }

    @Test
    void folgaEFeriadoSemBatidaNaoSaoFalta() {
        var folga = dia(LocalDate.of(2026, 10, 18), SituacaoDia.FOLGA, null, 0, 0, null, List.of(), List.of());
        var feriado = dia(LocalDate.of(2026, 10, 12), SituacaoDia.FERIADO, "N. Sra. Aparecida", 0, 0, null,
                List.of(), List.of());

        var r = somar(folga, feriado);

        assertThat(r.faltasNaoJustificadas()).isZero();
        assertThat(r.faltasJustificadas()).isZero();
    }

    // ---------- afastamento ----------

    @Test
    void afastamentoApareceMarcadoENaoViraFalta() {
        var ferias1 = dia(LocalDate.of(2026, 10, 5), SituacaoDia.AFASTAMENTO, "Férias", 0, 0, null,
                List.of(), List.of());
        var ferias2 = dia(LocalDate.of(2026, 10, 6), SituacaoDia.AFASTAMENTO, "Férias", 0, 0, null,
                List.of(), List.of());

        var r = somar(ferias1, ferias2);

        assertThat(r.diasAfastamento()).isEqualTo(2);
        assertThat(r.afastamentos()).containsExactly("Férias");
        assertThat(r.faltasNaoJustificadas()).isZero();
        assertThat(r.minutosPrevistos()).isZero();
    }

    // ---------- atrasos ----------

    @Test
    void atrasoSemJustificativaSomaMinutosENaoJustificados() {
        var r = somar(
                trabalhou(LocalDate.of(2026, 10, 13), 480, 470, 10),
                trabalhou(LocalDate.of(2026, 10, 14), 480, 465, 15));

        assertThat(r.atrasosNaoJustificados()).isEqualTo(2);
        assertThat(r.atrasosJustificados()).isZero();
        assertThat(r.minutosAtraso()).isEqualTo(25);
    }

    @Test
    void atrasoComJustificativaAprovadaEhJustificado() {
        var d = dia(LocalDate.of(2026, 10, 13), SituacaoDia.TRABALHO, null, 480, 450, 30,
                List.of(batida(StatusMarcacao.VALIDA)),
                List.of(justificativa(TipoOcorrencia.ATRASO, StatusSolicitacao.APROVADA)));

        var r = somar(d);

        assertThat(r.atrasosJustificados()).isEqualTo(1);
        assertThat(r.atrasosNaoJustificados()).isZero();
    }

    @Test
    void atrasoZeroNaoContaComoAtraso() {
        var r = somar(trabalhou(LocalDate.of(2026, 10, 13), 480, 480, 0));

        assertThat(r.atrasosNaoJustificados()).isZero();
        assertThat(r.minutosAtraso()).isZero();
    }

    // ---------- dia em aberto ----------

    @Test
    void diaComEntradaSemSaidaFicaForaDaContaEApareceEmAberto() {
        var d = dia(LocalDate.of(2026, 10, 16), SituacaoDia.TRABALHO, null, 480, null, null,
                List.of(batida(StatusMarcacao.VALIDA)), List.of());

        var r = somar(d);

        assertThat(r.diasEmAberto()).isEqualTo(1);
        assertThat(r.minutosPrevistos()).isZero();
        assertThat(r.minutosRealizados()).isZero();
        assertThat(r.faltasNaoJustificadas()).isZero();
    }

    // ---------- períodos ----------

    @Test
    void semanasVaoDeSegundaADomingoRecortadasNosLimites() {
        // 01/10/2026 é quinta-feira
        List<RelatorioPeriodoDTO> periodos = RelatorioCalculo.montarPeriodos(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 14), AgrupamentoRelatorio.SEMANA);

        assertThat(periodos).extracting(RelatorioPeriodoDTO::rotulo)
                .containsExactly("01/10 a 04/10", "05/10 a 11/10", "12/10 a 14/10");
    }

    @Test
    void semanaRecortadaEmUmDiaSoMostraSoAData() {
        // 02/11/2026 é segunda-feira
        List<RelatorioPeriodoDTO> periodos = RelatorioCalculo.montarPeriodos(
                LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2), AgrupamentoRelatorio.SEMANA);

        assertThat(periodos).extracting(RelatorioPeriodoDTO::rotulo)
                .containsExactly("26/10 a 01/11", "02/11");
    }

    @Test
    void mesesSaoRecortadosNoInicioENoFim() {
        List<RelatorioPeriodoDTO> periodos = RelatorioCalculo.montarPeriodos(
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 11, 10), AgrupamentoRelatorio.MES);

        assertThat(periodos).extracting(RelatorioPeriodoDTO::rotulo)
                .containsExactly("09/2026", "10/2026", "11/2026");
        assertThat(periodos.get(0).inicio()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(periodos.get(2).fim()).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void linhaSeparaOsDiasNoPeriodoCerto() {
        List<RelatorioPeriodoDTO> periodos = RelatorioCalculo.montarPeriodos(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 18), AgrupamentoRelatorio.SEMANA);
        RelatorioCalculo.Linha linha = new RelatorioCalculo.Linha(UUID.randomUUID(), "Ana", "001", periodos);

        linha.somarDia(trabalhou(LocalDate.of(2026, 10, 6), 480, 480, null), HOJE);
        linha.somarDia(trabalhou(LocalDate.of(2026, 10, 13), 480, 540, null), HOJE);

        var dto = linha.paraDTO();
        assertThat(dto.porPeriodo()).hasSize(2);
        assertThat(dto.porPeriodo().get(0).minutosRealizados()).isEqualTo(480);
        assertThat(dto.porPeriodo().get(1).minutosRealizados()).isEqualTo(540);
        assertThat(dto.total().minutosRealizados()).isEqualTo(1020);
        assertThat(dto.total().minutosExtras()).isEqualTo(60);
    }

    @Test
    void diaSemAlocacaoESemBatidaNaoEntraNoRelatorio() {
        var semAlocacao = dia(LocalDate.of(2026, 10, 1), SituacaoDia.SEM_ALOCACAO, null, 0, 0, null,
                List.of(), List.of());

        assertThat(RelatorioCalculo.diaRelevante(semAlocacao)).isFalse();
    }
}
