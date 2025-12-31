package com.koishman.telegram.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AliExpressProduct {
    private String productId;
    private String title;
    private String price;
    private String imageUrl;
    private String productUrl;
    private String salePrice;
    private Integer ordersCount;
    private Double rating;
}
