package com.wishlist.model.entity;

import com.wishlist.model.enums.Loja;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Column(name = "ultima_atualizacao")
    private LocalDateTime ultimaAtualizacao;

    @ManyToOne
    @JoinColumn(name = "lista_id", nullable = false)
    private Lista lista;

}
