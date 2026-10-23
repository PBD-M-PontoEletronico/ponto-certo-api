package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.model.Escala;
import com.mobdata.pontocerto.model.Marcacao;
import com.mobdata.pontocerto.model.ModelEscala;
import com.mobdata.pontocerto.model.ModoAberturaDispositivo;
import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.PoliticaForaPerimetro;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.model.TipoMarcacao;
import com.mobdata.pontocerto.model.Turno;
import com.mobdata.pontocerto.model.Usuario;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Pacote de abertura (W08): tudo o que o aparelho precisa guardar para poder
 * bater ponto sem rede, numa resposta só (GET /me/abertura).
 *
 * "modo" distingue os dois formatos possíveis:
 * - PESSOAL: "funcionario" vem preenchido, "funcionarios" vem vazio.
 * - RELOGIO_SETOR: "funcionario" vem nulo, "funcionarios" traz a lista de
 *   todos os funcionários alocados no setor daquela conta — e escalas,
 *   alocacoes, agenda e marcacoesRecentes vêm vazios (não fazem sentido numa
 *   tela compartilhada por várias pessoas).
 */
public record AberturaResponseDTO(
        Instant horaServidor,
        long horaServidorEpochMs,
        String fusoHorario,
        LocalDate periodoInicio,
        LocalDate periodoFim,
        ModoAberturaDispositivo modo,
        FuncionarioDTO funcionario,
        List<FuncionarioResumoDTO> funcionarios,
        List<SetorDTO> setores,
        List<EscalaDTO> escalas,
        List<AlocacaoDTO> alocacoes,
        List<EspelhoDiaDTO> agenda,
        List<MarcacaoRecenteDTO> marcacoesRecentes
) {

    public record FuncionarioDTO(
            UUID id,
            String nome,
            String usuario,
            Perfil perfil,
            String matricula,
            String cargo,
            UUID empresaId,
            String empresaNome
    ) {
        public static FuncionarioDTO fromEntity(Usuario u) {
            return new FuncionarioDTO(
                    u.getId(),
                    u.getNome(),
                    u.getUsuario(),
                    u.getPerfil(),
                    u.getMatricula(),
                    u.getCargo(),
                    u.getEmpresa() != null ? u.getEmpresa().getId() : null,
                    u.getEmpresa() != null ? u.getEmpresa().getRazaoSocial() : null
            );
        }
    }

    /** Versão enxuta do funcionário, usada na lista do modo relógio de setor. */
    public record FuncionarioResumoDTO(
            UUID id,
            String nome,
            String matricula
    ) {
        public static FuncionarioResumoDTO fromEntity(Usuario u) {
            return new FuncionarioResumoDTO(u.getId(), u.getNome(), u.getMatricula());
        }
    }

    /** Setor com a localização (para checar o perímetro offline) e a política. */
    public record SetorDTO(
            UUID id,
            String nome,
            String endereco,
            Double latitude,
            Double longitude,
            PoliticaDTO politica
    ) {
        public static SetorDTO fromEntity(Setor s) {
            return new SetorDTO(
                    s.getId(),
                    s.getNome(),
                    s.getEndereco(),
                    s.getLatitude(),
                    s.getLongitude(),
                    new PoliticaDTO(
                            s.getRaioMetros(),
                            s.isExigirSelfie(),
                            s.getPoliticaForaPerimetro(),
                            s.isIgnorarLocalizacao()
                    )
            );
        }
    }

    public record PoliticaDTO(
            Integer raioMetros,
            boolean exigirSelfie,
            PoliticaForaPerimetro politicaForaPerimetro,
            boolean ignorarLocalizacao
    ) {
    }

    public record EscalaDTO(
            UUID id,
            String nome,
            ModelEscala modelo,
            LocalDate dataReferencia,
            Set<Integer> diasSemana,
            List<TurnoDTO> turnos
    ) {
        public static EscalaDTO fromEntity(Escala e) {
            return new EscalaDTO(
                    e.getId(),
                    e.getNome(),
                    e.getModelo(),
                    e.getDataReferencia(),
                    e.getDiasSemana() == null ? Set.of() : new TreeSet<>(e.getDiasSemana()),
                    e.getTurnos().stream()
                            .sorted(Comparator.comparing(Turno::getHoraInicio))
                            .map(TurnoDTO::fromEntity)
                            .toList()
            );
        }
    }

    public record TurnoDTO(
            UUID id,
            LocalTime horaInicio,
            LocalTime horaFim,
            int intervaloMinutos,
            boolean atravessaMeiaNoite
    ) {
        public static TurnoDTO fromEntity(Turno t) {
            return new TurnoDTO(
                    t.getId(),
                    t.getHoraInicio(),
                    t.getHoraFim(),
                    t.getIntervaloMinutos(),
                    t.atravessaMeiaNoite()
            );
        }
    }

    /** Liga funcionário, setor e escala num intervalo de datas. */
    public record AlocacaoDTO(
            UUID id,
            UUID setorId,
            UUID escalaId,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        public static AlocacaoDTO fromEntity(Alocacao a) {
            return new AlocacaoDTO(
                    a.getId(),
                    a.getSetor().getId(),
                    a.getEscala().getId(),
                    a.getDataInicio(),
                    a.getDataFim()
            );
        }
    }

    /**
     * Marcação já registrada no servidor. "dataHora" é a hora real do toque
     * (a do aparelho corrigida pelo desvio do relógio, quando houver).
     */
    public record MarcacaoRecenteDTO(
            UUID id,
            TipoMarcacao tipo,
            Instant dataHora,
            UUID setorId
    ) {
        public static MarcacaoRecenteDTO fromEntity(Marcacao m) {
            return new MarcacaoRecenteDTO(
                    m.getId(),
                    m.getTipo(),
                    m.getHoraCorrigida(),
                    m.getSetor() != null ? m.getSetor().getId() : null
            );
        }
    }
}