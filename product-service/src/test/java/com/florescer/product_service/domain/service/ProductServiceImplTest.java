package com.florescer.product_service.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.florescer.product.api.dto.response.ProductGetResponse;
import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.enums.Status;
import com.florescer.product.domain.exception.personalizadas.ProductNotFoundException;
import com.florescer.product.domain.service.impl.ProductServiceImpl;
import com.florescer.product.infra.repository.ProductRepository;

class ProductServiceImplTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindByIdSuccess() {
        UUID id = UUID.randomUUID();

        Product product = Product.builder()
                .id(id)
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

        when(repository.findById(id)).thenReturn(Optional.of(product));

        // Mudança importante aqui: retorno agora é ProductGetResponse
        ProductGetResponse result = service.findById(id);

        // Ajustar chamada para .name() pois é um record
        assertEquals("Rosa", result.name());
        assertEquals("Flor", result.type());
        assertEquals("Flor vermelha", result.description());
        assertEquals(12.50, result.price());
    }

    @Test
    void testFindByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // Continua igual
        assertThrows(ProductNotFoundException.class, () -> service.findById(id));
    }
}
