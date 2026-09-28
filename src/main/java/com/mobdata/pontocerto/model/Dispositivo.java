package com.mobdata.pontocerto.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_dispositivo")
@Getter
@Setter
@NoArgsConstructor
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoDispositivo tipo;

    // Gerado e salvo no localStorage do navegador/tablet no primeiro acesso;
    // é o que identifica "este aparelho" nos próximos logins.
    @Column(name = "identificador", nullable = false)
    private String identificador;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Só um dispositivo ativo por usuário: logar em aparelho novo desativa o anterior.
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "data_vinculo", nullable = false)
    private LocalDateTime dataVinculo;

    @Column(name = "ultimo_acesso", nullable = false)
    private LocalDateTime ultimoAcesso;
}