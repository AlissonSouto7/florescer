package com.florescer.product.application.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.florescer.product.application.service.ProductService;
import com.florescer.product.domain.dto.ProductCreateRequest;
import com.florescer.product.domain.dto.ProductCreateResponse;
import com.florescer.product.domain.dto.ProductGetAllResponse;
import com.florescer.product.domain.dto.ProductGetResponse;
import com.florescer.product.domain.dto.ProductPatchRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Service Product", description = "Endpoints para CRUD de produtos")
public class ProductControllerImpl implements ProductController {
	
	private final ProductService service;

	@Override
	@Operation(summary = "Registra um novo produto", description = "Cria um produto e salva no DB")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Produto criado com sucesso"),
	    @ApiResponse(responseCode = "400", description = "Erro na requisição"),
	    @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
	public ResponseEntity<ProductCreateResponse> createProduct(
			@RequestBody @Parameter(description = "Dados do novo produto") ProductCreateRequest request) {
		ProductCreateResponse product = service.createProduct(request);
		return new ResponseEntity<>(product, HttpStatus.CREATED);
	}

	@Override
	@Operation(summary = "Lista todos os produtos", description = "Retorna uma lista de produtos cadastrados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de produtos retornada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
	public ResponseEntity<List<ProductGetAllResponse>> getAllProduct() {
		List<ProductGetAllResponse> productList = service.getListProduct();
		return new ResponseEntity<>(productList, HttpStatus.OK);
	}

	@Override
	@Operation(summary = "Busca um produto pelo ID", description = "Retorna os detalhes de um produto específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Produto encontrado"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
	public ResponseEntity<ProductGetResponse> getProductById(
			@Parameter(description = "ID do produto", required = true) UUID productId) {
		ProductGetResponse product = service.getProductById(productId);
		return new ResponseEntity<>(product, HttpStatus.OK);
	}

	@Override
	@Operation(summary = "Atualiza parcialmente um produto", description = "Permite a atualização parcial dos dados do produto")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Produto atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
        @ApiResponse(responseCode = "400", description = "Erro na requisição"),
        @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
	public ResponseEntity<Void> patchProduct(
			@Parameter(description = "ID do produto", required = true) UUID productId,
			@RequestBody(description = "Campos a serem atualizados") ProductPatchRequest request) {
		service.patchProduct(productId, request);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@Override
    @Operation(summary = "Remove um produto", description = "Exclui um produto do banco de dados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Produto removido com sucesso"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
	public ResponseEntity<Void> deleteProduct(@Parameter(description = "ID do produto", required = true) UUID productId) {
		service.deleteProduct(productId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}