package com.wishlist.scraper.base;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.List;

public abstract class BaseScraper {

    protected Document getDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(10_000)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao acessar página: " + url, e);
        }
    }

    protected BigDecimal extractPriceWithFallback(
            Document doc,
            List<String> selectors,
            String errorMessage
    ) {
        String rawPrice = findFirstText(doc, selectors);

        if (rawPrice == null) {
            throw new RuntimeException(errorMessage);
        }

        return parsePrice(rawPrice);
    }

    protected BigDecimal parsePrice(String raw) {
        return new BigDecimal(
                raw.replace("R$", "")
                        .replace("\u00A0", "")
                        .replace(".", "")
                        .replace(",", ".")
                        .trim()
        );
    }

    protected String extractNameWithFallback(
            Document doc,
            List<String> selectors,
            String errorMessage
    ) {
        String name = findFirstText(doc, selectors);

        if (name == null || name.isBlank()) {
            throw new RuntimeException(errorMessage);
        }

        return name.trim();
    }

    protected String extractImageWithFallback(
            Document doc,
            List<String> selectors
    ) {
        for (String selector : selectors) {
            Elements images = doc.select(selector);

            for (Element img : images) {
                String src = img.hasAttr("content")
                        ? img.attr("content")
                        : img.attr("src");

                if (src != null && !src.isBlank() && src.startsWith("http")) {
                    return src;
                }
            }
        }
        return null;
    }

    protected String findFirstText(Document doc, List<String> selectors) {
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                String text = elements.first().text();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }
}