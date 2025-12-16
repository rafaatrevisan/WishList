package com.wishlist.scraper.impl;

import com.wishlist.scraper.PriceScraper;
import com.wishlist.scraper.base.BaseScraper;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class KabumScraper extends BaseScraper implements PriceScraper {

    private static final List<String> PRICE_SELECTORS = List.of(
            "h4.text-4xl.text-secondary-500.font-bold",
            "h4.text-secondary-500",
            "[class*='text-secondary'][class*='font-bold']",
            "h4:matches(R\\$)"
    );

    private static final List<String> IMAGE_SELECTORS = List.of(
            "meta[property=og:image]",
            "img[src*='kabum']",
            "img[data-src*='kabum']",
            "img"
    );

    private static final List<String> NAME_SELECTORS = List.of(
            "h1.text-sm.desktop\\:text-xl.text-black-800.font-bold",
            "h1[class*='font-bold']",
            "h1"
    );

    @Override
    public boolean supports(String url) {
        return url.matches(".*kabum\\..*");
    }

    @Override
    public BigDecimal extractPrice(String url) {
        Document doc = getDocument(url);
        return extractPriceWithFallback(
                doc,
                PRICE_SELECTORS,
                "Preço não encontrado na página da Kabum"
        );
    }

    @Override
    public String extractImage(String url) {
        Document doc = getDocument(url);
        return extractImageWithFallback(doc, IMAGE_SELECTORS);
    }

    @Override
    public String extractName(String url) {
        Document doc = getDocument(url);
        return extractNameWithFallback(
                doc,
                NAME_SELECTORS,
                "Nome do produto não encontrado na Kabum"
        );
    }
}