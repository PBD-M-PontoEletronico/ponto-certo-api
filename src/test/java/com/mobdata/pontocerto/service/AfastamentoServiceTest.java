package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AfastamentoForm;
import com.mobdata.pontocerto.model.Afastamento;
import com.mobdata.pontocerto.model.Empresa;
import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.TipoAfastamento;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.AfastamentoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfastamentoServiceTest {

    @Mock
    private AfastamentoRepository afastamentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AnexoStorageService anexoStorageService;

    @InjectMocks
    private AfastamentoService afastamentoService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        Empresa empresa = new Empresa();
        empresa.setId(UUID.randomUUID());

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNome("Ana Funcionária");
        usuario.setEmpresa(empresa);

        // quem está chamando: RH da mesma empresa do funcionário
        TenantContext.set(empresa.getId(), Perfil.RH_ADMIN, UUID.randomUUID());

        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        lenient().when(afastamentoRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private AfastamentoForm form(LocalDate inicio, LocalDate fim) {
        AfastamentoForm form = new AfastamentoForm();
        form.setUsuarioId(usuario.getId());
        form.setTipo(TipoAfastamento.ATESTADO);
        form.setDataInicio(inicio);
        form.setDataFim(fim);
        return form;
    }

    @Test
    void recusaAfastamentoQueSobrepoeOutroDoMesmoFuncionario() {
        Afastamento existente = new Afastamento();
        existente.setUsuario(usuario);
        existente.setTipo(TipoAfastamento.FERIAS);
        existente.setDataInicio(LocalDate.of(2026, 10, 1));
        existente.setDataFim(LocalDate.of(2026, 10, 15));

        LocalDate inicio = LocalDate.of(2026, 10, 10);
        LocalDate fim = LocalDate.of(2026, 10, 20);
        when(afastamentoRepository.findSobrepostos(usuario.getId(), inicio, fim)).thenReturn(List.of(existente));

        assertThatThrownBy(() -> afastamentoService.cadastrar(form(inicio, fim)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Afastamento recusado")
                .hasMessageContaining("01/10/2026")
                .hasMessageContaining("15/10/2026");
    }

    @Test
    void aceitaAfastamentoQueNaoSobrepoeNenhumOutro() {
        LocalDate inicio = LocalDate.of(2026, 10, 16);
        LocalDate fim = LocalDate.of(2026, 10, 20);
        when(afastamentoRepository.findSobrepostos(usuario.getId(), inicio, fim)).thenReturn(List.of());

        Afastamento resultado = afastamentoService.cadastrar(form(inicio, fim));

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTipo()).isEqualTo(TipoAfastamento.ATESTADO);
        assertThat(resultado.getDataInicio()).isEqualTo(inicio);
        assertThat(resultado.getDataFim()).isEqualTo(fim);
        assertThat(resultado.getAnexoArquivo()).isNull();
    }

    @Test
    void recusaDataDeFimAnteriorAoInicio() {
        AfastamentoForm invalido = form(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 10));

        assertThatThrownBy(() -> afastamentoService.cadastrar(invalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data de fim");
    }
}
