package com.florescer.product.domain.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.exception.custom.FileStorageException;
import com.florescer.product.domain.exception.custom.InvalidPatchException;
import com.florescer.product.domain.exception.custom.ProductNotFoundException;
import com.florescer.product.domain.model.NewProduct;
import com.florescer.product.domain.model.ProductChanges;
import com.florescer.product.domain.service.ProductService;
import com.florescer.product.infra.repository.ProductRepository;
import com.florescer.product.infra.storage.FileCleanupOnCommit;
import com.florescer.product.infra.storage.ImageStorageService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ImageStorageService imageStorageService;
	private final FileCleanupOnCommit fileCleanup;
	private final ProductRepository repository;

	@Override
	public Product createProduct(NewProduct command, MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new FileStorageException("Imagem não enviada ou vazia");
		}

		String imagePath = imageStorageService.saveImage(image);

		return repository.save(Product.builder()
				.name(command.name())
				.type(command.type())
				.description(command.description())
				.price(command.price())
				.quantityStock(command.quantityStock())
				.careRequirements(command.careRequirements())
				.availability(command.availability())
				.status(command.status())
				.imagePath(imagePath)
				.build());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> getListProduct(Pageable pageable) {
		return repository.findAll(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Product getProductById(UUID id) {
		return getProductOrThrow(id);
	}

	@Override
	public void patchProduct(UUID id, ProductChanges changes, MultipartFile image) {
		Product product = getProductOrThrow(id);

		boolean semImagem = image == null || image.isEmpty();
		if (changes.isEmpty() && semImagem) {
			throw new InvalidPatchException("Nenhum campo foi enviado para atualização.");
		}

		aplicar(product, changes);

		if (!semImagem && !imageStorageService.isSameImage(product.getImagePath(), image)) {
			// Grava a nova primeiro: se a escrita falhar, o produto continua com
			// a imagem que tinha. A ordem inversa perdia as duas.
			String imagemAnterior = product.getImagePath();
			product.setImagePath(imageStorageService.saveImage(image));

			// A antiga só sai depois que o banco confirmar a troca.
			fileCleanup.deleteAfterCommit(imagemAnterior);
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

	private void aplicar(Product product, ProductChanges changes) {
		Optional.ofNullable(changes.name()).ifPresent(product::setName);
		Optional.ofNullable(changes.type()).ifPresent(product::setType);
		Optional.ofNullable(changes.description()).ifPresent(product::setDescription);
		Optional.ofNullable(changes.price()).ifPresent(product::setPrice);
		Optional.ofNullable(changes.quantityStock()).ifPresent(product::setQuantityStock);
		Optional.ofNullable(changes.careRequirements()).ifPresent(product::setCareRequirements);
		Optional.ofNullable(changes.availability()).ifPresent(product::setAvailability);
		Optional.ofNullable(changes.status()).ifPresent(product::setStatus);
	}

	private Product getProductOrThrow(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException(id));
	}
}
