package com.florescer.product_service.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.florescer.product.api.dto.request.ProductCreateRequest;
import com.florescer.product.api.dto.request.ProductPatchRequest;
import com.florescer.product.api.dto.response.ProductGetResponse;
import com.florescer.product.api.dto.response.ProductListResponse;
import com.florescer.product.api.utils.ProductMapper;
import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.enums.Status;

class ProductMapperTest {

    @Test
    void testToGetResponse() {
        Product product = createMockProduct();

        ProductGetResponse response = ProductMapper.toGetResponse(product);

        assertEquals(product.getId(), response.id());
        assertEquals(product.getName(), response.name());
        assertEquals(product.getPrice(), response.price());
    }

    @Test
    void testFromCreateRequest() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Orquídea", "Flor", "Delicada flor", 45.99,
                20, "Evitar sol direto", true, Status.ATIVO);

        String imagePath = "imagens/orquidea.jpg";

        Product product = ProductMapper.fromCreateRequest(request, imagePath);

        assertEquals("Orquídea", product.getName());
        assertEquals("Flor", product.getType());
        assertEquals("Delicada flor", product.getDescription());
        assertEquals(45.99, product.getPrice());
        assertEquals(imagePath, product.getImagePath());
    }

    @Test
    void testToListResponse() {
        Product product = createMockProduct();

        ProductListResponse response = ProductMapper.toListResponse(product);

        assertEquals(product.getId(), response.id());
        assertEquals(product.getName(), response.name());
        assertEquals(product.getPrice(), response.price());
    }

    @Test
    void testToGetAllResponse() {
        Product p1 = createMockProduct();
        Product p2 = createMockProduct();

        List<ProductListResponse> responseList = ProductMapper.toGetAllResponse(List.of(p1, p2));

        assertEquals(2, responseList.size());
        assertEquals(p1.getName(), responseList.get(0).name());
    }

    @Test
    void testApplyPatch() {
        Product product = createMockProduct();

        ProductPatchRequest patchRequest = new ProductPatchRequest(
                "Lírio", null, null, 99.90, null, null, Status.INATIVO);

        ProductMapper.applyPatch(product, patchRequest);

        assertEquals("Lírio", product.getName());
        assertEquals(99.90, product.getPrice());
        assertEquals(Status.INATIVO, product.getStatus());
    }

    // Utilitário para criar produto fake
    private Product createMockProduct() {
        return Product.builder()
                .id(UUID.randomUUID())
                .name("Rosa")
                .type("Flor")
                .description("Flor vermelha")
                .price(12.50)
                .quantityStock(10)
                .careRequirements("Regar diariamente")
                .availability(true)
                .status(Status.ATIVO)
                .imagePath("img.jpg")
                .build();
    }
}
