package com.mobdata.pontocerto.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tb_setor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Setor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "endereco", nullable = false)
    private String endereco;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    // Raio de aceite em metros — o perímetro dentro do qual a marcação vale.
    // Zero ou negativo é recusado na validação do SetorRequestDTO (@Positive).
    @Column(name = "raio_metros", nullable = false)
    private Integer raioMetros;

    // --- Política do setor (o que muda de setor para setor) ---

    @Column(name = "exigir_selfie", nullable = false)
    private boolean exigirSelfie;

    @Enumerated(EnumType.STRING)
    @Column(name = "politica_fora_perimetro", nullable = false)
    private PoliticaForaPerimetro politicaForaPerimetro;

    // Ignora a localização por inteiro (motorista, equipe de rua, campo aberto).
    // Quando true, o app não aplica o raio/perímetro desse setor na validação.
    @Column(name = "ignorar_localizacao", nullable = false)
    private boolean ignorarLocalizacao;

    // Isolamento multi-tenant: setor pertence a uma única empresa e nunca
    // aparece nem é acessível para outra (ver SetorService.verificarAcesso).
    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
}