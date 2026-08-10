package com.florescer.product.domain.service.impl;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.florescer.product.api.dto.request.ProductCreateRequest;
import com.florescer.product.api.dto.request.ProductPatchRequest;
import com.florescer.product.api.dto.response.ProductCreateResponse;
import com.florescer.product.api.dto.response.ProductGetResponse;
import com.florescer.product.api.dto.response.ProductListResponse;
import com.florescer.product.api.utils.ProductMapper;
import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.exception.personalizadas.FileStorageException;
import com.florescer.product.domain.exception.personalizadas.InvalidPatchException;
import com.florescer.product.domain.exception.personalizadas.ProductNotFoundException;
import com.florescer.product.domain.service.ProductService;
import com.florescer.product.infra.repository.ProductRepository;
import com.florescer.product.infra.storage.FileCleanupOnCommit;
import com.florescer.product.infra.storage.ImageStorageService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ImageStorageService imageStorageService;
	private final FileCleanupOnCommit fileCleanup;
	private final ProductRepository repository;

	@Override
	public ProductCreateResponse createProduct(ProductCreateRequest request, MultipartFile image) {
		
        if (image == null || image.isEmpty()) {
            throw new FileStorageException("Imagem não enviada ou vazia");
        }
		
		String imagePath = imageStorageService.saveImage(image);
		
		Product product = repository.save((ProductMapper.fromCreateRequest(request, imagePath)));
		return new ProductCreateResponse(product.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ProductListResponse> getListProduct(Pageable pageable) {
		Page<Product> listProduct = repository.findAll(pageable);
		return ProductMapper.toGetAllResponse(listProduct);
	}

	@Override
	@Transactional(readOnly = true)
	public ProductGetResponse getProductById(UUID id) {
		Product product = getProductOrThrow(id);
		return ProductMapper.toGetResponse(product);
	}

	@Override
	public void patchProduct(UUID id, ProductPatchRequest request, MultipartFile image) {
		Product product = getProductOrThrow(id);

        if (isRequestEmpty(request) && (image == null || image.isEmpty())) {
        	throw new InvalidPatchException("Nenhum campo foi enviado para atualização.");
        }
    
        ProductMapper.applyPatch(product, request);

        if (image != null && !image.isEmpty()) {
            boolean isSame = imageStorageService.isSameImage(product.getImagePath(), image);

            if (!isSame) {
                // Grava a nova primeiro: se a escrita falhar, o produto continua
                // com a imagem que tinha. A ordem inversa perdia as duas quando a
                // gravação falhava no meio.
                String imagemAnterior = product.getImagePath();
                String novoArquivo = imageStorageService.saveImage(image);
                product.setImagePath(novoArquivo);

                // A antiga só sai depois que o banco confirmar a troca.
                fileCleanup.deleteAfterCommit(imagemAnterior);
            }
        }
        repository.save(product);
	}

	@Override
	public void deleteProduct(UUID id) {
		Product product = getProductOrThrow(id);
		String imagem = product.getImagePath();

		repository.delete(product);

		// Se o banco desfizer a remoção, o produto continua existindo e precisa
		// da imagem: apagar antes deixaria uma referência apontando para o vazio.
		fileCleanup.deleteAfterCommit(imagem);
	}
	
    private Product getProductOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
	
	private boolean isRequestEmpty(ProductPatchRequest request) {
        return Stream.of(request.name(), request.type(), request.description(), request.price(),
                request.quantityStock(), request.careRequirements(), request.availability(), request.status())
                .allMatch(Objects::isNull);
    }
}