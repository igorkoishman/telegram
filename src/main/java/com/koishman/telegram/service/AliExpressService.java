package com.koishman.telegram.service;

import com.koishman.telegram.config.AliExpressConfig;
import com.koishman.telegram.model.AliExpressProduct;
import com.koishman.telegram.util.SslHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliExpressService {

    private final AliExpressConfig config;
    private final AliExpressApiService apiService;

    /**
     * Search for products on AliExpress
     * Strategy: Try API first, fall back to web scraping, then mock data
     * @param query Search query
     * @return List of products
     */
    public List<AliExpressProduct> searchProducts(String query) {
        // Try API first if enabled
        if (config.isEnabled()) {
            try {
                log.info("Attempting API search for: {}", query);
                List<AliExpressProduct> apiProducts = apiService.searchProducts(query);
                if (apiProducts != null && !apiProducts.isEmpty()) {
                    log.info("API search successful, returned {} products", apiProducts.size());
                    return apiProducts;
                }
                log.warn("API returned no products, falling back to web scraping");
            } catch (Exception e) {
                log.error("API search failed, falling back to web scraping", e);
            }
        }

        // Fallback to web scraping
        return searchProductsByScraping(query);
    }

    /**
     * Search for products on AliExpress using web scraping (fallback method)
     * @param query Search query
     * @return List of products
     */
    private List<AliExpressProduct> searchProductsByScraping(String query) {
        try {
            log.info("Scraping AliExpress for: {}", query);

            // Encode the search query for URL
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // Build AliExpress search URL
            String searchUrl = "https://www.aliexpress.com/wholesale?SearchText=" + encodedQuery;

            log.info("Fetching URL: {}", searchUrl);

            // Fetch and parse the page with SSL trust for Netskope compatibility
            log.info("Configuring Jsoup with trust-all SSL");
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(15000)
                    .sslSocketFactory(SslHelper.getTrustAllSocketFactory())
                    .ignoreHttpErrors(true)
                    .get();

            log.info("Successfully fetched page, status: {}", doc.connection().response().statusCode());

            // Parse products from the page
            List<AliExpressProduct> products = scrapeProductsFromPage(doc, query);

            if (products.isEmpty()) {
                log.warn("No products found by scraping, using mock data");
                return createMockProducts(query);
            }

            log.info("Successfully scraped {} products", products.size());
            return products;

        } catch (Exception e) {
            log.error("Failed to scrape AliExpress products: {}", e.getMessage(), e);
            log.info("Falling back to mock data");
            return createMockProducts(query);
        }
    }

    /**
     * Scrape products from the AliExpress search results page
     */
    private List<AliExpressProduct> scrapeProductsFromPage(Document doc, String query) {
        List<AliExpressProduct> products = new ArrayList<>();

        try {
            // Check if we got a captcha/bot detection page
            String pageText = doc.text().toLowerCase();
            if (pageText.contains("captcha") || pageText.contains("x5secdata") ||
                pageText.contains("punish") || doc.html().length() < 1000) {
                log.warn("AliExpress bot detection triggered - page contains captcha or is too small");
                return products; // Return empty, will trigger mock data
            }

            // AliExpress uses different selectors, let's try multiple approaches
            Elements productElements = doc.select("div[data-product-id]");

            if (productElements.isEmpty()) {
                // Try alternative selectors for newer AliExpress layout
                productElements = doc.select("a[href*='/item/']");
            }

            if (productElements.isEmpty()) {
                // Try another alternative
                productElements = doc.select("div.list-item, a.search-card-item");
            }

            log.info("Found {} product elements", productElements.size());

            for (Element element : productElements) {
                try {
                    AliExpressProduct product = extractProductFromElement(element);
                    if (product != null) {
                        products.add(product);
                        if (products.size() >= 5) {
                            break; // Limit to 5 products
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to extract product from element", e);
                }
            }

        } catch (Exception e) {
            log.error("Error scraping products from page", e);
        }

        return products;
    }

    /**
     * Extract product information from a single element
     */
    private AliExpressProduct extractProductFromElement(Element element) {
        try {
            // Extract product ID
            String productId = element.attr("data-product-id");
            if (productId.isEmpty()) {
                productId = extractFromUrl(element.select("a").first());
            }

            // Extract title
            String title = element.select("h1, h2, h3, .title, [class*=title]").text();
            if (title.isEmpty()) {
                title = element.select("a").attr("title");
            }

            // Extract price
            String price = extractPrice(element);

            // Extract image URL
            String imageUrl = element.select("img").first() != null
                ? element.select("img").first().attr("src")
                : "";
            if (imageUrl.isEmpty()) {
                imageUrl = element.select("img").first() != null
                    ? element.select("img").first().attr("data-src")
                    : "";
            }

            // Extract product URL
            String productUrl = element.select("a").first() != null
                ? element.select("a").first().attr("href")
                : "";
            if (!productUrl.startsWith("http")) {
                productUrl = "https:" + productUrl;
            }

            // Extract orders count
            Integer ordersCount = extractOrdersCount(element);

            // Extract rating
            Double rating = extractRating(element);

            // Only return if we have at least a title and price
            if (!title.isEmpty() && !price.isEmpty()) {
                return AliExpressProduct.builder()
                        .productId(productId)
                        .title(title)
                        .price(price)
                        .salePrice(null)
                        .imageUrl(imageUrl)
                        .productUrl(productUrl)
                        .ordersCount(ordersCount != null ? ordersCount : 0)
                        .rating(rating != null ? rating : 0.0)
                        .build();
            }

        } catch (Exception e) {
            log.debug("Failed to extract product info", e);
        }

        return null;
    }

    /**
     * Extract product ID from URL
     */
    private String extractFromUrl(Element linkElement) {
        if (linkElement == null) return "";

        String href = linkElement.attr("href");
        // AliExpress product URLs contain the ID like: /item/1234567890.html
        if (href.contains("/item/")) {
            String[] parts = href.split("/item/");
            if (parts.length > 1) {
                return parts[1].replaceAll("[^0-9]", "");
            }
        }
        return "";
    }

    /**
     * Extract price from element
     */
    private String extractPrice(Element element) {
        // Try multiple selectors for price
        String[] priceSelectors = {
            ".price", "[class*=price]", ".product-price",
            "span[class*=Price]", "div[class*=price]"
        };

        for (String selector : priceSelectors) {
            Elements priceElements = element.select(selector);
            if (!priceElements.isEmpty()) {
                String priceText = priceElements.first().text();
                if (priceText.matches(".*\\d+.*")) {
                    return "$" + priceText.replaceAll("[^0-9.]", "");
                }
            }
        }

        return "$9.99"; // Fallback
    }

    /**
     * Extract orders count from element
     */
    private Integer extractOrdersCount(Element element) {
        try {
            Elements orderElements = element.select("[class*=order], [class*=sold]");
            if (!orderElements.isEmpty()) {
                String orderText = orderElements.first().text();
                String numberOnly = orderText.replaceAll("[^0-9]", "");
                if (!numberOnly.isEmpty()) {
                    return Integer.parseInt(numberOnly);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract orders count", e);
        }
        return 0;
    }

    /**
     * Extract rating from element
     */
    private Double extractRating(Element element) {
        try {
            Elements ratingElements = element.select("[class*=rating], [class*=star]");
            if (!ratingElements.isEmpty()) {
                String ratingText = ratingElements.first().text();
                String ratingNumber = ratingText.replaceAll("[^0-9.]", "");
                if (!ratingNumber.isEmpty()) {
                    return Double.parseDouble(ratingNumber);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract rating", e);
        }
        return 4.5;
    }

    /**
     * Create mock products for testing/demo purposes with REAL working AliExpress links
     */
    private List<AliExpressProduct> createMockProducts(String query) {
        log.info("Creating search redirect for query: {}", query);

        // Use real AliExpress search URL that will work
        String searchUrl = "https://www.aliexpress.com/wholesale?SearchText=" +
                          java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);

        // Return a single item with clear instructions
        return Arrays.asList(
            AliExpressProduct.builder()
                .productId("redirect")
                .title("🔍 Click the link below to search AliExpress for: " + query)
                .price("$0.00")
                .salePrice(null)
                .imageUrl("")
                .productUrl(searchUrl)
                .ordersCount(0)
                .rating(0.0)
                .build()
        );
    }

    /**
     * Format product information for Telegram message
     */
    public String formatProductMessage(AliExpressProduct product) {
        // Check if this is mock data
        if (product.getPrice().equals("$0.00")) {
            return String.format(
                "🔍 %s\n\n" +
                "🔗 Link: %s",
                product.getTitle(),
                product.getProductUrl()
            );
        }

        return String.format(
            "🛍️ %s\n\n" +
            "💰 Price: %s %s\n" +
            "⭐ Rating: %.1f/5.0\n" +
            "📦 Orders: %d\n\n" +
            "🔗 Link: %s",
            product.getTitle(),
            product.getSalePrice() != null ? product.getSalePrice() : product.getPrice(),
            product.getSalePrice() != null ? "(was " + product.getPrice() + ")" : "",
            product.getRating(),
            product.getOrdersCount(),
            product.getProductUrl()
        );
    }

    /**
     * Format multiple products as a summary list
     */
    public String formatProductList(List<AliExpressProduct> products, String query) {
        if (products.isEmpty()) {
            return "❌ No products found for: " + query;
        }

        StringBuilder message = new StringBuilder();

        // Check if this is mock/redirect data
        boolean isMockData = !products.isEmpty() && products.get(0).getPrice().equals("$0.00");

        if (isMockData) {
            // Simple, clean message with search link
            message.append("🛍️ Searching AliExpress for: **").append(query).append("**\n\n");
            message.append("Due to AliExpress bot protection, please click the link below to view results:\n\n");
            message.append("🔗 ").append(products.get(0).getProductUrl());
        } else {
            message.append("🔍 Found ").append(products.size()).append(" products for: ").append(query).append("\n\n");

            for (int i = 0; i < Math.min(products.size(), 5); i++) {
                AliExpressProduct product = products.get(i);
                message.append(i + 1).append(". ").append(product.getTitle()).append("\n");
                message.append("   💰 ").append(product.getSalePrice() != null ? product.getSalePrice() : product.getPrice());
                message.append(" | ⭐ ").append(String.format("%.1f", product.getRating()));
                message.append(" | 📦 ").append(product.getOrdersCount()).append(" orders\n");
                message.append("   🔗 ").append(product.getProductUrl()).append("\n\n");
            }

            if (products.size() > 5) {
                message.append("... and ").append(products.size() - 5).append(" more results");
            }
        }

        return message.toString();
    }
}
