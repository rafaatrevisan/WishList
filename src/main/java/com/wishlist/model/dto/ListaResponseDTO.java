package com.wishlist.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListaResponseDTO {
    private Long id;
    private String nome;
    private String descricao;
    private LocalDateTime createdAt;
    private Integer totalProdutos;
}