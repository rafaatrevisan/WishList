package com.wishlist.service;

import com.wishlist.model.entity.Lista;
import com.wishlist.model.entity.Produto;
import com.wishlist.repository.ListaRepository;
import com.wishlist.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ListaRepository listaRepository;

    public ProdutoService(ProdutoRepository produtoRepository, ListaRepository listaRepository) {
        this.produtoRepository = produtoRepository;
        this.listaRepository = listaRepository;
    }

    public List<Produto> listarPorLista(Long listaId) {
        return produtoRepository.findByListaId(listaId);
    }

    public Produto adicionar(Long listaId, Produto produto) {
        Lista lista = listaRepository.findById(listaId)
                .orElseThrow(() -> new RuntimeException("Lista não encontrada"));

        produto.setLista(lista);
        produto.setUltimaAtualizacao(LocalDateTime.now());

        return produtoRepository.save(produto);
    }

    public void remover(Long produtoId) {
        produtoRepository.deleteById(produtoId);
    }

    public BigDecimal totalDaLista(Long listaId) {
        return produtoRepository.calcularTotalPorLista(listaId);
    }
}
