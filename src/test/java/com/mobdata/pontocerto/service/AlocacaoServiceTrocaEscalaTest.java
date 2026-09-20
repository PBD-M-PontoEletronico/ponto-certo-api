package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.TrocaEscalaRequestDTO;
import com.mobdata.pontocerto.dto.TrocaEscalaResponseDTO;
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
class AlocacaoServiceTrocaEscalaTest {

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
    private Setor setor;
    private Escala escalaAtual;
    private Escala escalaNova;
    private Alocacao atual;

    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(UUID.randomUUID());

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNome("Ana Funcionária");
        usuario.setEmpresa(empresa);

        setor = new Setor();
        setor.setId(UUID.randomUUID());
        setor.setNome("Setor A");
        setor.setEmpresa(empresa);

        escalaAtual = escalaDiurna("Comercial", LocalTime.of(8, 0), LocalTime.of(17, 0));
        escalaNova = escalaDiurna("Comercial tarde", LocalTime.of(8, 0), LocalTime.of(17, 0));

        // alocação vigente: começou há 5 dias e vai até daqui a 20
        atual = new Alocacao();
        atual.setId(UUID.randomUUID());
        atual.setUsuario(usuario);
        atual.setSetor(setor);
        atual.setEscala(escalaAtual);
        atual.setDataInicio(hoje.minusDays(5));
        atual.setDataFim(hoje.plusDays(20));

        when(alocacaoRepository.findById(atual.getId())).thenReturn(Optional.of(atual));
        when(setorService.buscarPorId(setor.getId())).thenReturn(setor);
        lenient().when(alocacaoRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    private Escala escalaDiurna(String nome, LocalTime inicio, LocalTime fim) {
        Escala escala = new Escala();
        escala.setEmpresa(empresa);
        escala.setNome(nome);
        escala.setModelo(ModelEscala.TURNO_DIURNO);
        escala.setTurnos(List.of(new Turno(inicio, fim, 0)));
        return escala;
    }

    private TrocaEscalaRequestDTO pedido(LocalDate dataTroca) {
        return new TrocaEscalaRequestDTO(UUID.randomUUID(), null, dataTroca, null);
    }

    @Test
    void trocaEncerraAAntigaNoDiaAnteriorEAbreANovaSemApagarNada() {
        LocalDate dataTroca = hoje.plusDays(1);
        TrocaEscalaRequestDTO request = pedido(dataTroca);

        when(escalaService.buscarPorId(request.escalaId())).thenReturn(escalaNova);
        // a antiga continua no banco (só encerrada), então ainda aparece na consulta
        when(alocacaoRepository.findAllByUsuarioId(usuario.getId())).thenReturn(List.of(atual));

        TrocaEscalaResponseDTO resultado = alocacaoService.trocarEscala(atual.getId(), request);

        assertThat(resultado.encerrada().dataInicio()).isEqualTo(hoje.minusDays(5));
        assertThat(resultado.encerrada().dataFim()).isEqualTo(dataTroca.minusDays(1));
        assertThat(resultado.nova().dataInicio()).isEqualTo(dataTroca);
        assertThat(resultado.nova().dataFim()).isEqualTo(hoje.plusDays(20));
        assertThat(resultado.nova().escala()).isEqualTo(escalaNova);
    }

    @Test
    void naoPermiteTrocaComDataNoPassado() {
        TrocaEscalaRequestDTO request = pedido(hoje.minusDays(1));

        assertThatThrownBy(() -> alocacaoService.trocarEscala(atual.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("passado não muda");
    }

    @Test
    void trocaPrecisaSerDepoisDoInicioDaAlocacaoAtual() {
        atual.setDataInicio(hoje.plusDays(3)); // alocação futura
        TrocaEscalaRequestDTO request = pedido(hoje.plusDays(3));

        assertThatThrownBy(() -> alocacaoService.trocarEscala(atual.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("depois do início");
    }

    @Test
    void recusaTrocaQuandoANovaEscalaConflitaComOutraAlocacaoDoFuncionario() {
        LocalDate dataTroca = hoje.plusDays(1);
        TrocaEscalaRequestDTO request = pedido(dataTroca);

        // outra alocação, em outro setor, no mesmo horário e nos dias da troca
        Setor setorB = new Setor();
        setorB.setId(UUID.randomUUID());
        setorB.setNome("Setor B");
        setorB.setEmpresa(empresa);

        Alocacao outra = new Alocacao();
        outra.setId(UUID.randomUUID());
        outra.setUsuario(usuario);
        outra.setSetor(setorB);
        outra.setEscala(escalaDiurna("Outra", LocalTime.of(9, 0), LocalTime.of(18, 0)));
        outra.setDataInicio(hoje.plusDays(2));
        outra.setDataFim(hoje.plusDays(10));

        when(escalaService.buscarPorId(request.escalaId())).thenReturn(escalaNova);
        when(alocacaoRepository.findAllByUsuarioId(usuario.getId())).thenReturn(List.of(atual, outra));

        assertThatThrownBy(() -> alocacaoService.trocarEscala(atual.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflito de alocação")
                .hasMessageContaining("Setor B");
    }
}
