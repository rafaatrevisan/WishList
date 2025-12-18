package com.wishlist.scraper.impl;

import com.wishlist.model.enums.Loja;
import com.wishlist.scraper.PriceScraper;
import com.wishlist.scraper.base.BaseScraper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MercadoLivreScraper extends BaseScraper implements PriceScraper {

    @Override
    public Loja getLoja() {
        return Loja.MERCADO_LIVRE;
    }

    private static final List<String> PRICE_SELECTORS = List.of(
            "meta[itemprop=price]",
            "span.andes-money-amount__fraction",
            "span:matches(R\\$)"
    );

    private static final List<String> NAME_SELECTORS = List.of(
            "h1.ui-pdp-title",
            "meta[property=og:title]",
            "h1"
    );

    private static final List<String> IMAGE_SELECTORS = List.of(
            "img[data-zoom]",
            "meta[property=og:image]",
            "img"
    );

    @Override
    public boolean supports(String url) {
        return url.contains("mercadolivre.com")
                || url.contains("mercadolivre.com.br")
                || url.contains("ml.com");
    }

    @Override
    public BigDecimal extractPrice(String url) {
        Document doc = getDocument(url);

        Element metaPrice = doc.selectFirst("meta[itemprop=price]");
        if (metaPrice != null) {
            String content = metaPrice.attr("content");
            if (content != null && !content.isBlank()) {
                return new BigDecimal(content);
            }
        }

        return extractPriceWithFallback(
                doc,
                PRICE_SELECTORS,
                "Preço não encontrado no Mercado Livre"
        );
    }

    @Override
    public String extractName(String url) {
        Document doc = getDocument(url);

        Element title = doc.selectFirst("h1.ui-pdp-title");
        if (title != null && !title.text().isBlank()) {
            return title.text().trim();
        }

        return extractNameWithFallback(
                doc,
                NAME_SELECTORS,
                "Nome do produto não encontrado no Mercado Livre"
        );
    }

    @Override
    public String extractImage(String url) {
        Document doc = getDocument(url);

        Element zoomImg = doc.selectFirst("img[data-zoom]");
        if (zoomImg != null) {
            String zoom = zoomImg.attr("data-zoom");
            if (zoom != null && !zoom.isBlank()) {
                return zoom;
            }
        }

        return extractImageWithFallback(
                doc,
                IMAGE_SELECTORS
        );
    }
}
