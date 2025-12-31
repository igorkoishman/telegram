package com.koishman.telegram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koishman.telegram.config.AliExpressConfig;
import com.koishman.telegram.model.AliExpressProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliExpressApiService {

    private final AliExpressApiClient apiClient;
    private final AliExpressConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Search for products using AliExpress Affiliate API
     * @param query Search query
     * @return List of products from API
     */
    public List<AliExpressProduct> searchProducts(String query) {
        try {
            log.info("Searching AliExpress API for: {}", query);

            // Build API request parameters
            Map<String, String> params = buildSearchRequest(query);

            // Execute API request
            String responseJson = apiClient.executeApiRequest(params);

            if (responseJson == null || responseJson.isEmpty()) {
                log.warn("Empty API response");
                return Collections.emptyList();
            }

            // Parse API response
            List<AliExpressProduct> products = parseApiResponse(responseJson, query);

            log.info("API returned {} products", products.size());
            return products;

        } catch (Exception e) {
            log.error("Failed to search products via API", e);
            return Collections.emptyList();
        }
    }

    /**
     * Build API request parameters for product search
     */
    private Map<String, String> buildSearchRequest(String query) {
        Map<String, String> params = new HashMap<>();

        // API method for AliExpress affiliate product query
        params.put("method", "aliexpress.affiliate.product.query");

        // Search keywords
        params.put("keywords", query);

        // Tracking ID for affiliate commissions
        if (config.getTrackingId() != null && !config.getTrackingId().isEmpty()) {
            params.put("tracking_id", config.getTrackingId());
        }

        // Target currency (USD)
        params.put("target_currency", "USD");

        // Target language (English)
        params.put("target_language", "EN");

        // Page size (max 50)
        params.put("page_size", "10");

        // Page number
        params.put("page_no", "1");

        // Sort by volume (sales)
        params.put("sort", "volume_desc");

        log.debug("API request params: {}", params);
        return params;
    }

    /**
     * Parse API JSON response to AliExpressProduct list
     */
    private List<AliExpressProduct> parseApiResponse(String responseJson, String query) {
        List<AliExpressProduct> products = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(responseJson);

            // Check for API errors
            if (root.has("error_response")) {
                JsonNode error = root.get("error_response");
                log.error("API error: {} - {}",
                    error.path("code").asText(),
                    error.path("msg").asText());
                return products;
            }

            // Navigate to product list
            // Response structure: aliexpress_affiliate_product_query_response > resp_result > result > products
            JsonNode response = root.path("aliexpress_affiliate_product_query_response");
            JsonNode respResult = response.path("resp_result");
            JsonNode result = respResult.path("result");
            JsonNode productsNode = result.path("products");

            if (productsNode.isObject()) {
                // Products might be wrapped in another object
                productsNode = productsNode.path("product");
            }

            if (!productsNode.isArray()) {
                log.warn("Products node is not an array: {}", productsNode.getNodeType());
                return products;
            }

            // Parse each product
            for (JsonNode productNode : productsNode) {
                try {
                    AliExpressProduct product = parseProduct(productNode);
                    if (product != null) {
                        products.add(product);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse product", e);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse API response", e);
        }

        return products;
    }

    /**
     * Parse single product from JSON node
     */
    private AliExpressProduct parseProduct(JsonNode productNode) {
        try {
            String productId = productNode.path("product_id").asText();
            String title = productNode.path("product_title").asText();

            // Price information
            String targetSalePrice = productNode.path("target_sale_price").asText();
            String targetOriginalPrice = productNode.path("target_original_price").asText();

            // Format prices
            String salePrice = !targetSalePrice.isEmpty() ? "$" + targetSalePrice : null;
            String originalPrice = !targetOriginalPrice.isEmpty() ? "$" + targetOriginalPrice : salePrice;

            // Image URL
            String imageUrl = productNode.path("product_main_image_url").asText();

            // Product URL (promotion link for affiliate)
            String productUrl = productNode.path("promotion_link").asText();
            if (productUrl.isEmpty()) {
                productUrl = productNode.path("product_detail_url").asText();
            }

            // Sales volume
            int volume = productNode.path("volume").asInt(0);

            // Rating
            JsonNode evalNode = productNode.path("evaluate_rate");
            double rating = 0.0;
            if (evalNode.isNumber()) {
                rating = evalNode.asDouble();
            } else if (evalNode.isTextual()) {
                try {
                    // Rating might be percentage like "95.5%" - convert to 0-5 scale
                    String rateText = evalNode.asText().replace("%", "");
                    double percentage = Double.parseDouble(rateText);
                    rating = percentage / 20.0; // Convert 0-100 to 0-5
                } catch (NumberFormatException e) {
                    rating = 4.5; // Default
                }
            }

            // Only return product if we have minimum required fields
            if (!title.isEmpty() && !productUrl.isEmpty()) {
                return AliExpressProduct.builder()
                        .productId(productId)
                        .title(title)
                        .price(originalPrice)
                        .salePrice(salePrice)
                        .imageUrl(imageUrl)
                        .productUrl(productUrl)
                        .ordersCount(volume)
                        .rating(rating)
                        .build();
            }

        } catch (Exception e) {
            log.debug("Failed to parse product node", e);
        }

        return null;
    }
}
