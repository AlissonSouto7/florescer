package com.florescer.product.api.controller.impl;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.florescer.product.api.controller.ProductController;
import com.florescer.product.api.dto.request.ProductCreateRequest;
import com.florescer.product.api.dto.request.ProductPatchRequest;
import com.florescer.product.api.dto.response.ProductCreateResponse;
import com.florescer.product.api.dto.response.ProductGetResponse;
import com.florescer.product.api.dto.response.ProductListResponse;
import com.florescer.product.api.utils.SwaggerConstants;
import com.florescer.product.config.SecurityConfig;
import com.florescer.product.domain.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Gerenciamento de produtos da floricultura")
@SecurityRequirement(name = SecurityConfig.SECURITY)
public class ProductControllerImpl implements ProductController {
	
	private final ObjectMapper objectMapper;
	private final ProductService service;

    @Override
    @Operation(summary = "Registra um novo produto", description = "Cria um produto e salva no DB")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = SwaggerConstants.STATUS_201),
        @ApiResponse(responseCode = "400", description = SwaggerConstants.STATUS_400),
        @ApiResponse(responseCode = "500", description = SwaggerConstants.STATUS_500)
    })
    //enviando string temporariamente para poder enviar as imagens pelo swagger
    public ResponseEntity<ProductCreateResponse> create(
        @Parameter(description = "JSON com os dados do produto", required = true)
        @Valid @Schema(implementation = ProductCreateRequest.class) String requestJson,
        @Parameter(description = "Imagem do produto (JPEG)", required = true)
        MultipartFile image
    ) {
        try {
            ProductCreateRequest request = objectMapper.readValue(requestJson, ProductCreateRequest.class);
            ProductCreateResponse product = service.createProduct(request, image);
            URI location = URI.create("/v1/product/" + product.id());
            return ResponseEntity.created(location).body(product);

        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON inválido", e);
        }
    }

	@Override
	@Operation(summary = "Lista todos os produtos", description = "Retorna uma lista de produtos cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SwaggerConstants.STATUS_200),
            @ApiResponse(responseCode = "500", description = SwaggerConstants.STATUS_500)
    })
    public ResponseEntity<Page<ProductListResponse>> listAll(
    	    @RequestParam(defaultValue = "0") int page,
    	    @RequestParam(defaultValue = "10") int size,
    	    @RequestParam(defaultValue = "name") String[] sort) {
    	    Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(service.getListProduct(pageable));
    }

	@Override
	@Operation(summary = "Busca um produto pelo ID", description = "Retorna os detalhes de um produto específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Produto encontrado"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
	public ResponseEntity<ProductGetResponse> findById(
			@Parameter(description = "ID do produto", required = true) UUID id, HttpServletRequest request) {
		ProductGetResponse product = service.getProductById(id, request);
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
    public ResponseEntity<Void> updatePartial(@Parameter(description = "ID do produto", required = true) UUID id,
    	    @Parameter(description = "JSON com os campos a serem atualizados", required = true)
    	    @Schema(implementation = ProductPatchRequest.class) String patchJson, 
    	    @Parameter(description = "Nova imagem do produto (opcional)", required = false) 
            MultipartFile image) {
		try {
	        ProductPatchRequest patchRequest = objectMapper.readValue(patchJson, ProductPatchRequest.class);
	        service.patchProduct(id, patchRequest, image);
	        return ResponseEntity.noContent().build();
	    } catch (JsonProcessingException e) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON inválido", e);
	    }
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