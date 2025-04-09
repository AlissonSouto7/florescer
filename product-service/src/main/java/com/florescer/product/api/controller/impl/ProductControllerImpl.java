package com.florescer.product.api.controller.impl;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.florescer.product.api.controller.ProductController;
import com.florescer.product.api.dto.request.ProductCreateRequest;
import com.florescer.product.api.dto.request.ProductPatchRequest;
import com.florescer.product.api.dto.response.ProductCreateResponse;
import com.florescer.product.api.dto.response.ProductGetResponse;
import com.florescer.product.api.dto.response.ProductListResponse;
import com.florescer.product.api.utils.SwaggerConstants;
import com.florescer.product.domain.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Gerenciamento de produtos da floricultura")
public class ProductControllerImpl implements ProductController {
	
	private final ProductService service;

	@Override
	@Operation(summary = "Registra um novo produto", description = "Cria um produto e salva no DB")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = SwaggerConstants.STATUS_201),
            @ApiResponse(responseCode = "400", description = SwaggerConstants.STATUS_400),
            @ApiResponse(responseCode = "500", description = SwaggerConstants.STATUS_500)
    })
    public ResponseEntity<ProductCreateResponse> createProduct(
            @RequestPart("product") ProductCreateRequest request,
            @RequestPart("image") MultipartFile image) {
        ProductCreateResponse product = service.createProduct(request, image);
        URI location = URI.create("/v1/product/" + product.id());
        return ResponseEntity.created(location).body(product);
    }

	@Override
	@Operation(summary = "Lista todos os produtos", description = "Retorna uma lista de produtos cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SwaggerConstants.STATUS_200),
            @ApiResponse(responseCode = "500", description = SwaggerConstants.STATUS_500)
    })
    public ResponseEntity<List<ProductListResponse>> listAll() {
        return ResponseEntity.ok(service.getListProduct());
    }

	@Override
	@Operation(summary = "Busca um produto pelo ID", description = "Retorna os detalhes de um produto específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Produto encontrado"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
	public ResponseEntity<ProductGetResponse> findById(
			@Parameter(description = "ID do produto", required = true) UUID id) {
		ProductGetResponse product = service.getProductById(id);
		return ResponseEntity.ok(product);
	}

	@Override
	@Operation(summary = "Atualiza parcialmente um produto", description = "Permite a atualização parcial dos dados do produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = SwaggerConstants.STATUS_204),
            @ApiResponse(responseCode = "400", description = SwaggerConstants.STATUS_400),
            @ApiResponse(responseCode = "404", description = SwaggerConstants.STATUS_404),
            @ApiResponse(responseCode = "500", description = SwaggerConstants.STATUS_500)
    })
    public ResponseEntity<Void> updatePartial(
            @Parameter(description = "ID do produto", required = true)UUID id,
            @RequestBody ProductPatchRequest request) {
        service.patchProduct(id, request);
        return ResponseEntity.noContent().build();
    }
	@Override
    @Operation(summary = "Remove um produto", description = "Exclui um produto do banco de dados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = SwaggerConstants.STATUS_204),
            @ApiResponse(responseCode = "404", description = SwaggerConstants.STATUS_404),
            @ApiResponse(responseCode = "500", description = SwaggerConstants.STATUS_500)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do produto", required = true)UUID id) {
        service.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}