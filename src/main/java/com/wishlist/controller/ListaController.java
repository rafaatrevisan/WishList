package com.wishlist.controller;

import com.wishlist.model.dto.ListaResponseDTO;
import com.wishlist.model.entity.Lista;
import com.wishlist.service.ListaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/listas")
@CrossOrigin
public class ListaController {

    private final ListaService listaService;

    public ListaController(ListaService listaService) {
        this.listaService = listaService;
    }

    @GetMapping
    public List<ListaResponseDTO> listar() {
        List<Lista> listas = listaService.listarTodas();

        return listas.stream()
                .map(lista -> {
                    ListaResponseDTO dto = new ListaResponseDTO();
                    dto.setId(lista.getId());
                    dto.setNome(lista.getNome());
                    dto.setDescricao(lista.getDescricao());
                    dto.setCreatedAt(lista.getCreatedAt());
                    dto.setTotalProdutos(lista.getProdutos().size());
                    return dto;
                })
                .collect(Collectors.toList());
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
