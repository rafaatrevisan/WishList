package com.wishlist.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wishlist.model.enums.Loja;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.*;

@Entity
@Table(name = "produto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String link;

    @Enumerated(EnumType.STRING)
    private Loja loja;

    @Column(name = "preco_atual", precision = 10, scale = 2)
    private BigDecimal precoAtual;

    @Column(name = "imagem_url", columnDefinition = "TEXT")
    private String imagemUrl;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "ultima_atualizacao")
    private LocalDateTime ultimaAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_id", nullable = false)
    @JsonIgnoreProperties("produtos")
    private Lista lista;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProdutoPrecoHistorico> historicos;

    @PrePersist
    public void prePersist() {
        this.dataCriacao = LocalDateTime.now();
    }

}
