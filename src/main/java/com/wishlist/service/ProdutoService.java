package com.wishlist.service;

import com.wishlist.model.dto.ProdutoRequestDTO;
import com.wishlist.model.dto.ProdutoResponseDTO;
import com.wishlist.model.entity.Lista;
import com.wishlist.model.entity.Produto;
import com.wishlist.repository.ListaRepository;
import com.wishlist.repository.ProdutoRepository;
import com.wishlist.scraper.PriceScraper;
import com.wishlist.scraper.ScraperFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ListaRepository listaRepository;
    private final ScraperFactory scraperFactory;

    public ProdutoService(
            ProdutoRepository produtoRepository,
            ListaRepository listaRepository,
            ScraperFactory scraperFactory
    ) {
        this.produtoRepository = produtoRepository;
        this.listaRepository = listaRepository;
        this.scraperFactory = scraperFactory;
    }

    /* CRIAÇÃO DE PRODUTO */
    public ProdutoResponseDTO adicionar(ProdutoRequestDTO dto) {

        if (dto.getLink() == null || dto.getLink().isBlank()) {
            throw new IllegalArgumentException("Link do produto é obrigatório");
        }

        Lista lista = listaRepository.findById(dto.getListaId())
                .orElseThrow(() -> new RuntimeException("Lista não encontrada"));

        Produto produto = new Produto();
        produto.setLink(dto.getLink());
        produto.setLista(lista);

        boolean temScraper = hasScraper(dto.getLink());

        if (temScraper) {
            preencherComScraper(produto, dto);
        } else {
            validarCamposObrigatoriosSemScraper(dto);
            preencherManual(produto, dto);
        }

        produto.setUltimaAtualizacao(LocalDateTime.now());

        Produto salvo = produtoRepository.save(produto);
        return mapToResponseDTO(salvo);
    }

    /* LÓGICA COM SCRAPER */
    private void preencherComScraper(
            Produto produto,
            ProdutoRequestDTO dto
    ) {
        PriceScraper scraper = scraperFactory.getScraper(dto.getLink());

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            produto.setNome(dto.getNome());
        } else {
            String nome = scraper.extractName(dto.getLink());
            if (nome == null || nome.isBlank()) {
                throw new RuntimeException("Não foi possível extrair o nome do produto");
            }
            produto.setNome(nome);
        }
        produto.setLoja(
                dto.getLoja() != null
                        ? dto.getLoja()
                        : extractLoja(dto.getLink())
        );
        produto.setPrecoAtual(
                dto.getPrecoAtual() != null
                        ? dto.getPrecoAtual()
                        : scraper.extractPrice(dto.getLink())
        );
        produto.setImagemUrl(
                dto.getImagemUrl() != null
                        ? dto.getImagemUrl()
                        : scraper.extractImage(dto.getLink())
        );
    }

    /* LÓGICA SEM SCRAPER */
    private void validarCamposObrigatoriosSemScraper(ProdutoRequestDTO dto) {
        if (dto.getNome() == null ||
                dto.getLoja() == null ||
                dto.getPrecoAtual() == null ||
                dto.getImagemUrl() == null) {

            throw new IllegalArgumentException(
                    "Para lojas sem scraper, nome, loja, preço e imagem são obrigatórios"
            );
        }
    }

    private void preencherManual(Produto produto, ProdutoRequestDTO dto) {
        produto.setNome(dto.getNome());
        produto.setLoja(dto.getLoja());
        produto.setPrecoAtual(dto.getPrecoAtual());
        produto.setImagemUrl(dto.getImagemUrl());
    }

    /* ATUALIZAÇÕES */
    public ProdutoResponseDTO atualizarPreco(Long produtoId, BigDecimal novoPreco) {

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        produto.setPrecoAtual(novoPreco);
        produto.setUltimaAtualizacao(LocalDateTime.now());

        Produto salvo = produtoRepository.save(produto);
        return mapToResponseDTO(salvo);
    }

    public ProdutoResponseDTO atualizarPrecoAutomatico(Long produtoId) {

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        PriceScraper scraper = scraperFactory.getScraper(produto.getLink());

        produto.setPrecoAtual(scraper.extractPrice(produto.getLink()));
        produto.setImagemUrl(scraper.extractImage(produto.getLink()));

        if (produto.getNome() == null || produto.getNome().isBlank()) {
            produto.setNome(scraper.extractName(produto.getLink()));
        }

        produto.setUltimaAtualizacao(LocalDateTime.now());

        Produto salvo = produtoRepository.save(produto);
        return mapToResponseDTO(salvo);
    }

    public List<ProdutoResponseDTO> atualizarPrecosDaLista(Long listaId) {

        List<Produto> produtos = produtoRepository.findByListaId(listaId);
        List<ProdutoResponseDTO> atualizados = new ArrayList<>();

        for (Produto produto : produtos) {
            try {
                PriceScraper scraper =
                        scraperFactory.getScraper(produto.getLink());

                produto.setPrecoAtual(scraper.extractPrice(produto.getLink()));
                produto.setImagemUrl(scraper.extractImage(produto.getLink()));

                if (produto.getNome() == null || produto.getNome().isBlank()) {
                    produto.setNome(scraper.extractName(produto.getLink()));
                }

                produto.setUltimaAtualizacao(LocalDateTime.now());

                Produto salvo = produtoRepository.save(produto);
                atualizados.add(mapToResponseDTO(salvo));

            } catch (Exception e) {
                System.err.println(
                        "Erro ao atualizar produto ID " + produto.getId()
                                + ": " + e.getMessage()
                );
            }
        }

        return atualizados;
    }

    /* MÉTODOS AUXILIARES */
    private boolean hasScraper(String link) {
        try {
            scraperFactory.getScraper(link);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractLoja(String link) {
        if (link.contains("amazon")) return "AMAZON";
        if (link.contains("kabum")) return "KABUM";
        if (link.contains("shopee")) return "SHOPEE";
        return "DESCONHECIDA";
    }

    public List<Produto> listarPorLista(Long listaId) {
        return produtoRepository.findByListaId(listaId);
    }

    public void remover(Long produtoId) {
        produtoRepository.deleteById(produtoId);
    }

    public BigDecimal totalDaLista(Long listaId) {
        return produtoRepository.calcularTotalPorLista(listaId);
    }

    private ProdutoResponseDTO mapToResponseDTO(Produto produto) {
        ProdutoResponseDTO dto = new ProdutoResponseDTO();
        dto.setId(produto.getId());
        dto.setNome(produto.getNome());
        dto.setLink(produto.getLink());
        dto.setLoja(produto.getLoja());
        dto.setPrecoAtual(produto.getPrecoAtual());
        dto.setImagemUrl(produto.getImagemUrl());
        dto.setUltimaAtualizacao(produto.getUltimaAtualizacao());
        dto.setListaId(produto.getLista().getId());
        dto.setListaNome(produto.getLista().getNome());
        return dto;
    }
}