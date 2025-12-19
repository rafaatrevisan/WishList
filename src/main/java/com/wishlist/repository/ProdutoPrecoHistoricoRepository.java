package com.wishlist.repository;

import com.wishlist.model.entity.ProdutoPrecoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProdutoPrecoHistoricoRepository
        extends JpaRepository<ProdutoPrecoHistorico, Long> {

    Optional<ProdutoPrecoHistorico>
    findTopByProdutoIdOrderByDataColetaDesc(Long produtoId);

    boolean existsByProdutoId(Long produtoId);

    void deleteByProdutoId(Long produtoId);

    List<ProdutoPrecoHistorico> findByProdutoIdAndDataColetaBetweenOrderByDataColetaAsc(
            Long produtoId,
            LocalDateTime inicio,
            LocalDateTime fim
    );

}
