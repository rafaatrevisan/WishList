package com.wishlist.service;

import com.wishlist.model.dto.ProdutoRequestDTO;
import com.wishlist.model.dto.ProdutoResponseDTO;
import com.wishlist.model.entity.Lista;
import com.wishlist.model.entity.Produto;
import com.wishlist.model.enums.Loja;
import com.wishlist.repository.ListaRepository;
import com.wishlist.repository.ProdutoRepository;
import com.wishlist.scraper.PriceScraper;
import com.wishlist.scraper.ScraperFactory;
import com.wishlist.model.entity.ProdutoPrecoHistorico;
import com.wishlist.repository.ProdutoPrecoHistoricoRepository;
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
    private final ProdutoPrecoHistoricoRepository produtoPrecoHistoricoRepository;

    public ProdutoService(
            ProdutoRepository produtoRepository,
            ListaRepository listaRepository,
            ScraperFactory scraperFactory,
            ProdutoPrecoHistoricoRepository produtoPrecoHistoricoRepository
    ) {
        this.produtoRepository = produtoRepository;
        this.listaRepository = listaRepository;
        this.scraperFactory = scraperFactory;
        this.produtoPrecoHistoricoRepository = produtoPrecoHistoricoRepository;
    }

    public ProdutoResponseDTO adicionar(ProdutoRequestDTO dto) {

        if (dto.getLink() == null || dto.getLink().isBlank()) {
            throw new IllegalArgumentException("Link do produto é obrigatório");
        }

        Lista lista = listaRepository.findById(dto.getListaId())
                .orElseThrow(() -> new RuntimeException("Lista não encontrada"));

        Produto produto = new Produto();
        produto.setLink(dto.getLink());
        produto.setLista(lista);

        if (hasScraper(dto.getLink())) {
            preencherComScraper(produto, dto);
        } else {
            validarCamposObrigatoriosSemScraper(dto);
            preencherManual(produto, dto);
            produto.setLoja(Loja.DESCONHECIDA);
        }

        produto.setUltimaAtualizacao(LocalDateTime.now());

        Produto salvo = produtoRepository.save(produto);

        salvarHistoricoPreco(salvo, salvo.getPrecoAtual());

        return mapToResponseDTO(salvo);
    }

    private void preencherComScraper(
            Produto produto,
            ProdutoRequestDTO dto
    ) {
        PriceScraper scraper = scraperFactory.getScraper(dto.getLink());

        produto.setLoja(scraper.getLoja());

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            produto.setNome(dto.getNome());
        } else {
            String nome = scraper.extractName(dto.getLink());
            if (nome == null || nome.isBlank()) {
                throw new RuntimeException("Não foi possível extrair o nome do produto");
            }
            produto.setNome(nome);
        }

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
        produto.setLoja(Loja.valueOf(dto.getLoja()));
        produto.setPrecoAtual(dto.getPrecoAtual());
        produto.setImagemUrl(dto.getImagemUrl());
    }

    public ProdutoResponseDTO atualizarProduto(Long produtoId, ProdutoRequestDTO dto) {

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            produto.setNome(dto.getNome());
        }

        if (dto.getImagemUrl() != null && !dto.getImagemUrl().isBlank()) {
            produto.setImagemUrl(dto.getImagemUrl());
        }

        if (dto.getLoja() != null && !dto.getLoja().isBlank()) {
            produto.setLoja(Loja.valueOf(dto.getLoja()));
        }

        if (dto.getPrecoAtual() != null) {
            salvarHistoricoPreco(produto, dto.getPrecoAtual());
            produto.setPrecoAtual(dto.getPrecoAtual());
        }

        produto.setUltimaAtualizacao(LocalDateTime.now());

        Produto salvo = produtoRepository.save(produto);
        return mapToResponseDTO(salvo);
    }

    public ProdutoResponseDTO atualizarPrecoAutomatico(Long produtoId) {

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        PriceScraper scraper = scraperFactory.getScraper(produto.getLink());

        BigDecimal novoPreco = scraper.extractPrice(produto.getLink());

        salvarHistoricoPreco(produto, novoPreco);

        produto.setPrecoAtual(novoPreco);
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
                PriceScraper scraper = scraperFactory.getScraper(produto.getLink());

                BigDecimal novoPreco = scraper.extractPrice(produto.getLink());

                salvarHistoricoPreco(produto, novoPreco);

                produto.setPrecoAtual(novoPreco);
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

    // Método que usa Job para atualizar preço e imagem
    public void atualizarPrecosAutomaticamente() {

        List<Produto> produtos = produtoRepository.findAll();

        for (Produto produto : produtos) {

            try {
                PriceScraper scraper = scraperFactory.getScraper(produto.getLink());

                BigDecimal novoPreco = scraper.extractPrice(produto.getLink());

                salvarHistoricoPreco(produto, novoPreco);

                if (novoPreco != null) {
                    produto.setPrecoAtual(novoPreco);
                }

                String imagem = scraper.extractImage(produto.getLink());
                if (imagem != null && !imagem.isBlank()) {
                    produto.setImagemUrl(imagem);
                }

                produto.setUltimaAtualizacao(LocalDateTime.now());
                produtoRepository.save(produto);

            } catch (Exception e) {
                System.err.println(
                        "[JOB] Falha ao atualizar produto ID "
                                + produto.getId()
                                + " | Loja: "
                                + produto.getLoja()
                                + " | Erro: "
                                + e.getMessage()
                );
            }
        }
    }

    private void salvarHistoricoPreco(Produto produto, BigDecimal novoPreco) {

        if (novoPreco == null) {
            return;
        }

        BigDecimal precoAtual = produto.getPrecoAtual();

        // Se não tem preço atual OU se o preço mudou, salva no histórico
        if (precoAtual == null || precoAtual.compareTo(novoPreco) != 0) {

            ProdutoPrecoHistorico historico = new ProdutoPrecoHistorico();
            historico.setProduto(produto);
            historico.setPreco(novoPreco);
            historico.setLoja(produto.getLoja().name());

            produtoPrecoHistoricoRepository.save(historico);

            System.out.println(
                    "[HISTÓRICO] Produto ID " + produto.getId() +
                            " | De: R$ " + precoAtual +
                            " | Para: R$ " + novoPreco
            );
        }
    }

    public List<Produto> listarPorLista(Long listaId) {
        return produtoRepository.findByListaId(listaId);
    }

    public BigDecimal totalDaLista(Long listaId) {
        return produtoRepository.calcularTotalPorLista(listaId);
    }

    public void remover(Long produtoId) {
        produtoRepository.deleteById(produtoId);
    }

    /* AUX */
    private boolean hasScraper(String link) {
        try {
            scraperFactory.getScraper(link);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private ProdutoResponseDTO mapToResponseDTO(Produto produto) {
        ProdutoResponseDTO dto = new ProdutoResponseDTO();
        dto.setId(produto.getId());
        dto.setNome(produto.getNome());
        dto.setLink(produto.getLink());
        dto.setLoja(String.valueOf(produto.getLoja()));
        dto.setPrecoAtual(produto.getPrecoAtual());
        dto.setImagemUrl(produto.getImagemUrl());
        dto.setUltimaAtualizacao(produto.getUltimaAtualizacao());
        dto.setListaId(produto.getLista().getId());
        dto.setListaNome(produto.getLista().getNome());
        return dto;
    }
}
