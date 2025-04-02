package com.florescer.product.domain.dto;

import java.util.List;

import com.florescer.product.domain.entity.Product;

public class ProductMapper {
	public static Product toEntity(ProductCreateRequest request) {
        return new Product(request);
    }

    public static ProductGetResponse toGetResponse(Product product) {
        return new ProductGetResponse(product.getProductId(),
                product.getName(),
                product.getType(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantityStock(),
                product.getCareRequirements(),
                product.getAvailability(),
                product.getStatus());
    }

    public static List<ProductGetAllResponse> toGetAllResponse(List<Product> products) {
        return products.stream().map(ProductGetAllResponse::from).toList();
    }
}
