package com.florescer.product.api.utils;

import org.springframework.data.domain.Page;

import com.florescer.product.api.dto.request.ProductCreateRequest;
import com.florescer.product.api.dto.request.ProductPatchRequest;
import com.florescer.product.api.dto.response.ProductCreateResponse;
import com.florescer.product.api.dto.response.ProductGetResponse;
import com.florescer.product.api.dto.response.ProductListResponse;
import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.model.NewProduct;
import com.florescer.product.domain.model.ProductChanges;
import com.florescer.product.infra.storage.ImageStorageService;

/**
 * Converte entre o contrato HTTP e o domínio.
 *
 * <p>A tradução acontece na borda: o serviço recebe comandos e devolve
 * entidades, sem saber que existe JSON. Antes o próprio serviço chamava este
 * mapeador, o que fazia o domínio depender da camada de apresentação.
 */
public final class ProductMapper {

    private ProductMapper() {
    }

    public static NewProduct toCommand(ProductCreateRequest request) {
        return new NewProduct(
                request.name(),
                request.type(),
                request.description(),
                request.price(),
                request.quantityStock(),
                request.careRequirements(),
                request.availability(),
                request.status(),
                request.heightCm(),
                request.light(),
                request.watering(),
                request.petSafe(),
                request.environment(),
                request.difficulty(),
                request.includesPot());
    }

    public static ProductChanges toChanges(ProductPatchRequest request) {
        return new ProductChanges(
                request.name(),
                request.type(),
                request.description(),
                request.price(),
                request.quantityStock(),
                request.careRequirements(),
                request.availability(),
                request.status(),
                request.heightCm(),
                request.light(),
                request.watering(),
                request.petSafe(),
                request.environment(),
                request.difficulty(),
                request.includesPot());
    }

    public static ProductCreateResponse toCreateResponse(Product product) {
        return new ProductCreateResponse(product.getId());
    }

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
                ImageStorageService.buildImageUrl(product.getImagePath()),
                product.getHeightCm(),
                product.getLight(),
                product.getWatering(),
                product.getPetSafe(),
                product.getEnvironment(),
                product.getDifficulty(),
                product.getIncludesPot());
    }

    public static Page<ProductListResponse> toListResponse(Page<Product> products) {
        return products.map(ProductMapper::toListItem);
    }

    /**
     * A listagem devolve o mesmo formato de imagem que o detalhe: o campo tem o
     * mesmo nome nos dois endpoints, então precisa ter o mesmo significado.
     */
    private static ProductListResponse toListItem(Product product) {
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
                ImageStorageService.buildImageUrl(product.getImagePath()),
                product.getHeightCm(),
                product.getLight(),
                product.getWatering(),
                product.getPetSafe(),
                product.getEnvironment(),
                product.getDifficulty(),
                product.getIncludesPot());
    }
}
