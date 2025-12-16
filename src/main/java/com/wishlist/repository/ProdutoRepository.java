package com.wishlist.repository;

import com.wishlist.model.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByListaId(Long listaId);

    @Query("SELECT COALESCE(SUM(p.precoAtual), 0) FROM Produto p WHERE p.lista.id = :listaId")
    BigDecimal calcularTotalPorLista(Long listaId);
}
