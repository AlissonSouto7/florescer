package com.florescer.product.api.utils;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.florescer.product.api.dto.request.ProductCreateRequest;
import com.florescer.product.api.dto.request.ProductPatchRequest;
import com.florescer.product.api.dto.response.ProductGetResponse;
import com.florescer.product.api.dto.response.ProductListResponse;
import com.florescer.product.domain.entity.Product;
import com.florescer.product.infra.storage.ImageStorageService;

public class ProductMapper {

    public static ProductGetResponse toGetResponse(Product product) {
        return new ProductGetResponse(product.getId(),
                product.getName(),
                product.getType(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantityStock(),
                product.getCareRequirements(),
                product.getAvailability(),
                product.getStatus(),
                ImageStorageService.buildImageUrl(product.getImagePath()));
    }
    
    public static Product fromCreateRequest(ProductCreateRequest request, String imagePath) {
        return Product.builder()
                .name(request.name())
                .type(request.type())
                .description(request.description())
                .price(request.price())
                .quantityStock(request.quantityStock())
                .careRequirements(request.careRequirements())
                .availability(request.availability())
                .status(request.status())
                .imagePath(imagePath)
                .build();
    }
    
    public static Page<ProductListResponse> toGetAllResponse(Page<Product> products) {
        return products.map(ProductMapper::toListResponse);
    }

    /**
     * A listagem devolve o mesmo formato de imagem que o detalhe.
     *
     * <p>Antes o detalhe trazia URL e a listagem trazia só o nome do arquivo,
     * ambos no campo chamado imageUrl: o cliente precisava saber de qual
     * endpoint o dado veio para saber como usá-lo.
     */
    public static ProductListResponse toListResponse(Product product) {
        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getType(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantityStock(),
                product.getCareRequirements(),
                product.getAvailability(),
                product.getStatus(),
                ImageStorageService.buildImageUrl(product.getImagePath()));
    }
    
    public static void applyPatch(Product product, ProductPatchRequest request) {
        Optional.ofNullable(request.name()).ifPresent(product::setName);
        Optional.ofNullable(request.type()).ifPresent(product::setType);
        Optional.ofNullable(request.description()).ifPresent(product::setDescription);
        Optional.ofNullable(request.price()).ifPresent(product::setPrice);
        Optional.ofNullable(request.quantityStock()).ifPresent(product::setQuantityStock);
        Optional.ofNullable(request.careRequirements()).ifPresent(product::setCareRequirements);
        Optional.ofNullable(request.availability()).ifPresent(product::setAvailability);
        Optional.ofNullable(request.status()).ifPresent(product::setStatus);
    }
}