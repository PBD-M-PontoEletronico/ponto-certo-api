package com.mobdata.pontocerto.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

// Afastamento é da PESSOA (férias, atestado, licença), sempre com início e fim.
@Entity
@Table(name = "tb_afastamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Afastamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoAfastamento tipo;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    // Anexo opcional (atestado, comprovante...). O arquivo fica em disco; aqui
    // guardamos só o nome com que foi salvo, o nome original e o tipo.
    @Column(name = "anexo_arquivo")
    private String anexoArquivo;

    @Column(name = "anexo_nome_original")
    private String anexoNomeOriginal;

    @Column(name = "anexo_tipo_conteudo")
    private String anexoTipoConteudo;
}
