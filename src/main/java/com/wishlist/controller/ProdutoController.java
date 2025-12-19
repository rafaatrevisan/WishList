package com.wishlist.controller;

import com.wishlist.model.dto.ProdutoPrecoHistoricoResponseDTO;
import com.wishlist.model.dto.ProdutoRequestDTO;
import com.wishlist.model.dto.ProdutoResponseDTO;
import com.wishlist.model.entity.Produto;
import com.wishlist.service.ProdutoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/produtos")
@CrossOrigin
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    // ==================== CRUD BÁSICO ====================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponseDTO criar(@RequestBody ProdutoRequestDTO dto) {
        return produtoService.adicionar(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody ProdutoRequestDTO dto
    ) {
        ProdutoResponseDTO response = produtoService.atualizarProduto(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        produtoService.remover(id);
    }

    // ==================== LISTAGEM ====================

    @GetMapping("/lista/{listaId}")
    public ResponseEntity<List<Produto>> listarPorLista(@PathVariable Long listaId) {
        List<Produto> produtos = produtoService.listarPorLista(listaId);
        return ResponseEntity.ok(produtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) {
        ProdutoResponseDTO produto = produtoService.buscarPorId(id);
        return ResponseEntity.ok(produto);
    }

    // ==================== ATUALIZAÇÕES DE PREÇO ====================

    @PutMapping("/{id}/atualizar-preco-automatico")
    public ResponseEntity<ProdutoResponseDTO> atualizarPrecoAutomatico(
            @PathVariable Long id
    ) {
        ProdutoResponseDTO response = produtoService.atualizarPrecoAutomatico(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/lista/{listaId}/atualizar-precos")
    public ResponseEntity<List<ProdutoResponseDTO>> atualizarPrecosDaLista(
            @PathVariable Long listaId
    ) {
        List<ProdutoResponseDTO> produtos = produtoService.atualizarPrecosDaLista(listaId);
        return ResponseEntity.ok(produtos);
    }

    // ==================== HISTÓRICO ====================

    @GetMapping("/{produtoId}/historico")
    public ResponseEntity<List<ProdutoPrecoHistoricoResponseDTO>> obterHistoricoPreco(
            @PathVariable Long produtoId,
            @RequestParam("dataInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam("dataFim") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        List<ProdutoPrecoHistoricoResponseDTO> historico = produtoService.obterHistoricoPreco(
                produtoId,
                dataInicio.atStartOfDay(),
                dataFim.atTime(23, 59, 59)
        );
        return ResponseEntity.ok(historico);
    }

    // ==================== CÁLCULOS ====================

    @GetMapping("/lista/{listaId}/total")
    public ResponseEntity<Map<String, BigDecimal>> calcularTotalLista(
            @PathVariable Long listaId
    ) {
        BigDecimal total = produtoService.totalDaLista(listaId);
        return ResponseEntity.ok(Map.of("total", total));
    }

    // ==================== EXCEPTION HANDLERS ====================

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleRuntimeException(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }
}