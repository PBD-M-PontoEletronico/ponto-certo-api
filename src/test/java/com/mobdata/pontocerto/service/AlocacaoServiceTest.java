package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AlocacaoRequestDTO;
import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.model.Empresa;
import com.mobdata.pontocerto.model.Escala;
import com.mobdata.pontocerto.model.ModelEscala;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.model.Turno;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.AlocacaoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlocacaoServiceTest {

    @Mock
    private AlocacaoRepository alocacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SetorService setorService;

    @Mock
    private EscalaService escalaService;

    @InjectMocks
    private AlocacaoService alocacaoService;

    private Empresa empresa;
    private Usuario usuario;
    private Setor setorA;
    private Setor setorB;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(UUID.randomUUID());

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNome("Ana Funcionária");
        usuario.setEmpresa(empresa);

        setorA = new Setor();
        setorA.setId(UUID.randomUUID());
        setorA.setNome("Setor A");
        setorA.setEmpresa(empresa);

        setorB = new Setor();
        setorB.setId(UUID.randomUUID());
        setorB.setNome("Setor B");
        setorB.setEmpresa(empresa);

        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        lenient().when(alocacaoRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    private Escala escalaTurnoDiurno(String nome, LocalTime inicio, LocalTime fim) {
        Escala escala = new Escala();
        escala.setEmpresa(empresa);
        escala.setNome(nome);
        escala.setModelo(ModelEscala.TURNO_DIURNO);
        escala.setTurnos(List.of(new Turno(inicio, fim, 0)));
        return escala;
    }

    private Alocacao alocacaoExistente(Setor setor, Escala escala, LocalDate inicio, LocalDate fim) {
        Alocacao alocacao = new Alocacao();
        alocacao.setId(UUID.randomUUID());
        alocacao.setUsuario(usuario);
        alocacao.setSetor(setor);
        alocacao.setEscala(escala);
        alocacao.setDataInicio(inicio);
        alocacao.setDataFim(fim);
        return alocacao;
    }

    private AlocacaoRequestDTO requestPara(Setor setor, Escala escala, LocalDate inicio, LocalDate fim) {
        return new AlocacaoRequestDTO(usuario.getId(), setor.getId(), UUID.randomUUID(), inicio, fim);
    }

    @Test
    void turnoQueAtravessaMeiaNoiteConflitaComTurnoDaMadrugadaSeguinte() {
        Escala escalaNoturna = escalaTurnoDiurno("Noturno 19x7", LocalTime.of(19, 0), LocalTime.of(7, 0));
        Escala escalaDiurna = escalaTurnoDiurno("Diurno 6x14", LocalTime.of(6, 0), LocalTime.of(14, 0));

        Alocacao existente = alocacaoExistente(setorB, escalaNoturna,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        when(alocacaoRepository.findAllByUsuarioId(usuario.getId())).thenReturn(List.of(existente));

        AlocacaoRequestDTO request = requestPara(setorA, escalaDiurna,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15));
        when(setorService.buscarPorId(setorA.getId())).thenReturn(setorA);
        when(escalaService.buscarPorId(request.escalaId())).thenReturn(escalaDiurna);

        assertThatThrownBy(() -> alocacaoService.alocar(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflito de alocação")
                .hasMessageContaining("2026-01-14");
    }

    @Test
    void alocacaoEmSetoresDiferentesNoMesmoHorarioConflita() {
        Escala escalaExistente = escalaTurnoDiurno("Comercial", LocalTime.of(8, 0), LocalTime.of(17, 0));
        Escala escalaCandidata = escalaTurnoDiurno("Comercial 2", LocalTime.of(9, 0), LocalTime.of(18, 0));

        Alocacao existente = alocacaoExistente(setorA, escalaExistente,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        when(alocacaoRepository.findAllByUsuarioId(usuario.getId())).thenReturn(List.of(existente));

        AlocacaoRequestDTO request = requestPara(setorB, escalaCandidata,
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 4, 10));
        when(setorService.buscarPorId(setorB.getId())).thenReturn(setorB);
        when(escalaService.buscarPorId(request.escalaId())).thenReturn(escalaCandidata);

        assertThatThrownBy(() -> alocacaoService.alocar(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Setor A");
    }

    @Test
    void encostarSemSobreporEhAceito() {
        Escala escalaDiurna = escalaTurnoDiurno("Diurno 7x19", LocalTime.of(7, 0), LocalTime.of(19, 0));
        Escala escalaNoturna = escalaTurnoDiurno("Noturno 19x7", LocalTime.of(19, 0), LocalTime.of(7, 0));

        Alocacao existente = alocacaoExistente(setorA, escalaDiurna,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        when(alocacaoRepository.findAllByUsuarioId(usuario.getId())).thenReturn(List.of(existente));

        AlocacaoRequestDTO request = requestPara(setorB, escalaNoturna,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        when(setorService.buscarPorId(setorB.getId())).thenReturn(setorB);
        when(escalaService.buscarPorId(request.escalaId())).thenReturn(escalaNoturna);

        Alocacao resultado = alocacaoService.alocar(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEscala()).isEqualTo(escalaNoturna);
    }

    @Test
    void periodosQueNaoSeTocamNaoConflitam() {
        Escala escalaExistente = escalaTurnoDiurno("Comercial", LocalTime.of(8, 0), LocalTime.of(17, 0));
        Escala escalaCandidata = escalaTurnoDiurno("Comercial 2", LocalTime.of(8, 0), LocalTime.of(17, 0));

        Alocacao existente = alocacaoExistente(setorA, escalaExistente,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        when(alocacaoRepository.findAllByUsuarioId(usuario.getId())).thenReturn(List.of(existente));

        AlocacaoRequestDTO request = requestPara(setorB, escalaCandidata,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        when(setorService.buscarPorId(setorB.getId())).thenReturn(setorB);
        when(escalaService.buscarPorId(request.escalaId())).thenReturn(escalaCandidata);

        Alocacao resultado = alocacaoService.alocar(request);

        assertThat(resultado).isNotNull();
    }
}
