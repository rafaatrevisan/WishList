package com.wishlist.scraper;

import java.math.BigDecimal;

public interface PriceScraper {

    boolean supports(String url);

    BigDecimal extractPrice(String url);

    String extractImage(String url);

    String extractName(String url);
}
