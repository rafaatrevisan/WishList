package com.wishlist.scraper.impl;

import com.wishlist.scraper.PriceScraper;
import com.wishlist.scraper.base.BaseScraper;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AmazonScraper extends BaseScraper implements PriceScraper {

    private static final List<String> PRICE_SELECTORS = List.of(
            "span.a-price span.a-offscreen",
            "span.a-price-whole",
            "span:matches(R\\$)"
    );

    private static final List<String> IMAGE_SELECTORS = List.of(
            "meta[property=og:image]",
            "#imgTagWrapperId img",
            "img[data-old-hires]",
            "img"
    );

    private static final List<String> NAME_SELECTORS = List.of(
            "#productTitle",
            "h1#title span",
            "h1 span",
            "h1"
    );

    @Override
    public boolean supports(String url) {
        return url.matches(".*amazon\\..*");
    }

    @Override
    public BigDecimal extractPrice(String url) {
        Document doc = getDocument(url);

        String rawPrice = findFirstText(doc, List.of("span.a-price span.a-offscreen"));
        if (rawPrice != null) {
            return parsePrice(rawPrice);
        }

        Elements whole = doc.select("span.a-price-whole");
        Elements fraction = doc.select("span.a-price-fraction");

        if (!whole.isEmpty() && !fraction.isEmpty()) {
            String raw = "R$" +
                    whole.first().text().replace(".", "").replace(",", "") +
                    "," +
                    fraction.first().text();

            return parsePrice(raw);
        }

        return extractPriceWithFallback(
                doc,
                PRICE_SELECTORS,
                "Preço não encontrado na página da Amazon"
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
                "Nome do produto não encontrado na Amazon"
        );
    }
}