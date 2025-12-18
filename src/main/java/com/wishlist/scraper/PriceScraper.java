package com.wishlist.scraper;

import com.wishlist.model.enums.Loja;

import java.math.BigDecimal;

public interface PriceScraper {

    boolean supports(String url);

    Loja getLoja();

    BigDecimal extractPrice(String url);

    String extractName(String url);

    String extractImage(String url);
}
