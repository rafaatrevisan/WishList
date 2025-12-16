package com.wishlist.service;

import com.wishlist.model.entity.Lista;
import com.wishlist.repository.ListaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListaService {

    private final ListaRepository listaRepository;

    public ListaService(ListaRepository listaRepository) {
        this.listaRepository = listaRepository;
    }

    public List<Lista> listarTodas() {
        return listaRepository.findAll();
    }

    public Lista criar(Lista lista) {
        return listaRepository.save(lista);
    }

    public void remover(Long id) {
        listaRepository.deleteById(id);
    }
}
