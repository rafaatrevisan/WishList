package com.wishlist.scraper.impl;

import com.wishlist.model.enums.Loja;
import com.wishlist.scraper.PriceScraper;
import com.wishlist.scraper.base.BaseScraper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AliExpressScraper extends BaseScraper implements PriceScraper {

    @Override
    public Loja getLoja() {
        return Loja.ALIEXPRESS;
    }

    private static final List<String> PRICE_SELECTORS = List.of(
            "div.product-price-value",
            "span.product-price-value",
            "div.product-price-current",
            "span[class*=snow-price]",
            "div[class*=price] span",
            "meta[property=product:price:amount]",
            "meta[name=twitter:data1]"
    );

    private static final List<String> NAME_SELECTORS = List.of(
            "h1[data-pl=product-title]",
            "h1.product-title-text",
            "meta[property=og:title]",
            "meta[name=twitter:title]",
            "title"
    );

    private static final List<String> IMAGE_SELECTORS = List.of(
            "meta[property=og:image]",
            "meta[name=twitter:image]",
            "img.magnifier-image",
            "img[class*=magnifier]",
            "div.image-view-magnifier-wrap img"
    );

    @Override
    public boolean supports(String url) {
        return url.contains("aliexpress.com")
                || url.contains("aliexpress.us")
                || url.contains("pt.aliexpress.com");
    }

    @Override
    public BigDecimal extractPrice(String url) {
        Document doc = getDocument(url);

        // 1. Tenta extrair dos parâmetros da URL
        BigDecimal urlPrice = extractPriceFromUrl(url);
        if (urlPrice != null && isValidPrice(urlPrice)) {
            return urlPrice;
        }

        // 2. Tenta extrair de meta tags
        BigDecimal metaPrice = extractPriceFromMetaTags(doc);
        if (metaPrice != null && isValidPrice(metaPrice)) {
            return metaPrice;
        }

        // 3. Tenta extrair de JSON-LD
        BigDecimal jsonLdPrice = extractPriceFromJsonLd(doc);
        if (jsonLdPrice != null && isValidPrice(jsonLdPrice)) {
            return jsonLdPrice;
        }

        // 4. Tenta extrair de scripts window.runParams
        BigDecimal scriptPrice = extractPriceFromScripts(doc);
        if (scriptPrice != null && isValidPrice(scriptPrice)) {
            return scriptPrice;
        }

        // 5. Tenta pelos seletores CSS
        String rawPrice = findFirstText(doc, PRICE_SELECTORS);
        if (rawPrice != null && !rawPrice.isBlank()) {
            BigDecimal cssPrice = parsePriceAliExpress(rawPrice);
            if (cssPrice != null && isValidPrice(cssPrice)) {
                return cssPrice;
            }
        }

        // 6. Busca padrões de preço no HTML completo
        BigDecimal htmlPrice = extractPriceFromHtml(doc);
        if (htmlPrice != null && isValidPrice(htmlPrice)) {
            return htmlPrice;
        }

        throw new RuntimeException("Preço não encontrado no AliExpress. A página pode estar protegida ou usar renderização JavaScript avançada.");
    }

    @Override
    public String extractName(String url) {
        Document doc = getDocument(url);

        Element titleElement = doc.selectFirst("h1[data-pl=product-title]");
        if (titleElement != null && !titleElement.text().isBlank()) {
            return titleElement.text().trim();
        }

        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null) {
            String content = ogTitle.attr("content");
            if (content != null && !content.isBlank()) {
                return content.replaceAll("\\s*-\\s*AliExpress.*$", "").trim();
            }
        }

        Element twitterTitle = doc.selectFirst("meta[name=twitter:title]");
        if (twitterTitle != null) {
            String content = twitterTitle.attr("content");
            if (content != null && !content.isBlank()) {
                return content.replaceAll("\\s*-\\s*AliExpress.*$", "").trim();
            }
        }

        Element title = doc.selectFirst("title");
        if (title != null && !title.text().isBlank()) {
            return title.text().replaceAll("\\s*-\\s*AliExpress.*$", "").trim();
        }

        throw new RuntimeException("Nome do produto não encontrado no AliExpress");
    }

    @Override
    public String extractImage(String url) {
        Document doc = getDocument(url);

        Element ogImage = doc.selectFirst("meta[property=og:image]");
        if (ogImage != null) {
            String content = ogImage.attr("content");
            if (content != null && !content.isBlank() && content.startsWith("http")) {
                return content;
            }
        }

        Element twitterImage = doc.selectFirst("meta[name=twitter:image]");
        if (twitterImage != null) {
            String content = twitterImage.attr("content");
            if (content != null && !content.isBlank() && content.startsWith("http")) {
                return content;
            }
        }

        return extractImageWithFallback(doc, IMAGE_SELECTORS);
    }

    /* Métodos auxiliares específicos do AliExpress */
    private BigDecimal extractPriceFromUrl(String url) {
        try {
            // AliExpress coloca preços nos parâmetros da URL
            // Exemplo: pdp_npi=5%40dis%21BRL%21BRL%20170.49%21BRL%20153.49
            Pattern pattern = Pattern.compile("BRL%20([0-9]+\\.?[0-9]*)");
            Matcher matcher = pattern.matcher(url);

            BigDecimal lastPrice = null;
            while (matcher.find()) {
                String priceStr = matcher.group(1);
                lastPrice = new BigDecimal(priceStr);
            }

            // Retorna o último preço encontrado (geralmente é o com desconto)
            return lastPrice;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(new BigDecimal("1.00")) > 0;
    }

    private BigDecimal extractPriceFromMetaTags(Document doc) {
        try {
            // Tenta product:price:amount
            Element priceMeta = doc.selectFirst("meta[property=product:price:amount]");
            if (priceMeta != null) {
                String content = priceMeta.attr("content");
                if (content != null && !content.isBlank()) {
                    return new BigDecimal(content.trim());
                }
            }

            // Tenta twitter:data1
            Element twitterData = doc.selectFirst("meta[name=twitter:data1]");
            if (twitterData != null) {
                String content = twitterData.attr("content");
                if (content != null && content.matches(".*\\d+[.,]\\d+.*")) {
                    return parsePriceAliExpress(content);
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    private BigDecimal extractPriceFromJsonLd(Document doc) {
        try {
            Element jsonLd = doc.selectFirst("script[type=application/ld+json]");
            if (jsonLd != null) {
                String json = jsonLd.html();

                // Procura por padrões de preço no JSON
                Pattern pattern = Pattern.compile("\"price\"\\s*:\\s*\"?([0-9.]+)\"?");
                Matcher matcher = pattern.matcher(json);

                if (matcher.find()) {
                    return new BigDecimal(matcher.group(1));
                }

                // Tenta lowPrice ou highPrice
                Pattern offerPattern = Pattern.compile("\"(?:low|high)Price\"\\s*:\\s*\"?([0-9.]+)\"?");
                Matcher offerMatcher = offerPattern.matcher(json);

                if (offerMatcher.find()) {
                    return new BigDecimal(offerMatcher.group(1));
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    private BigDecimal extractPriceFromScripts(Document doc) {
        try {
            for (Element script : doc.select("script:not([src])")) {
                String scriptContent = script.html();

                // Procura por padrões comuns de preço em scripts
                Pattern patterns[] = {
                        Pattern.compile("\"price\"\\s*:\\s*\"?([0-9.]+)\"?"),
                        Pattern.compile("\"minPrice\"\\s*:\\s*\"?([0-9.]+)\"?"),
                        Pattern.compile("\"maxPrice\"\\s*:\\s*\"?([0-9.]+)\"?"),
                        Pattern.compile("\"actMinPrice\"\\s*:\\s*\"?([0-9.]+)\"?"),
                        Pattern.compile("price[\"']?\\s*:\\s*[\"']?([0-9.]+)[\"']?")
                };

                for (Pattern pattern : patterns) {
                    Matcher matcher = pattern.matcher(scriptContent);
                    if (matcher.find()) {
                        String priceStr = matcher.group(1);
                        if (priceStr != null && !priceStr.isBlank()) {
                            return new BigDecimal(priceStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    private BigDecimal extractPriceFromHtml(Document doc) {
        try {
            String html = doc.html();

            // Procura por padrões de preço em todo HTML
            Pattern[] patterns = {
                    Pattern.compile("R\\$\\s*([0-9]{2,}[.,][0-9]{2})"),
                    Pattern.compile("US\\$\\s*([0-9]{2,}[.,][0-9]{2})"),
                    Pattern.compile("BRL\\s*([0-9]{2,}[.,][0-9]{2})"),
                    Pattern.compile("\\$\\s*([0-9]{2,}[.,][0-9]{2})")
            };

            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(html);
                if (matcher.find()) {
                    String priceStr = matcher.group(1);
                    BigDecimal price = parsePriceAliExpress(priceStr);
                    if (price != null && isValidPrice(price)) {
                        return price;
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    private BigDecimal parsePriceAliExpress(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String cleaned = raw
                .replace("R$", "")
                .replace("US$", "")
                .replace("BRL", "")
                .replace("$", "")
                .replace("\u00A0", "")
                .replace(" ", "")
                .trim();

        if (cleaned.matches(".*\\d+,\\d{2}$")) {
            // Formato brasileiro: 1.234,56
            cleaned = cleaned.replace(".", "").replace(",", ".");
        } else if (cleaned.matches(".*\\d+\\.\\d{2}$")) {
            // Formato americano: 1,234.56
            cleaned = cleaned.replace(",", "");
        }

        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}