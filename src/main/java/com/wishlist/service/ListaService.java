package com.wishlist.service;

import com.wishlist.model.entity.Lista;
import com.wishlist.repository.ListaRepository;
import com.wishlist.repository.ProdutoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListaService {

    private final ListaRepository listaRepository;
    private final ProdutoRepository produtoRepository;

    public ListaService(ListaRepository listaRepository, ProdutoRepository produtoRepository) {
        this.listaRepository = listaRepository;
        this.produtoRepository = produtoRepository;
    }

    public List<Lista> listarTodas() {
        return listaRepository.findAll();
    }

    public Lista criar(Lista lista) {
        return listaRepository.save(lista);
    }

    @Transactional
    public void remover(Long id) {
        Lista lista = listaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lista não encontrada"));

        listaRepository.deleteById(id);
    }
}
