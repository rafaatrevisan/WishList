package com.wishlist.controller;

import com.wishlist.model.entity.Lista;
import com.wishlist.service.ListaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/listas")
@CrossOrigin
public class ListaController {

    private final ListaService listaService;

    public ListaController(ListaService listaService) {
        this.listaService = listaService;
    }

    @GetMapping
    public List<Lista> listar() {
        return listaService.listarTodas();
    }

    @PostMapping
    public Lista criar(@RequestBody Lista lista) {
        return listaService.criar(lista);
    }

    @DeleteMapping("/{id}")
    public void remover(@PathVariable Long id) {
        listaService.remover(id);
    }
}
