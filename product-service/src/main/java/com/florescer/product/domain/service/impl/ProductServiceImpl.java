package com.florescer.product.domain.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.exception.custom.FileStorageException;
import com.florescer.product.domain.exception.custom.InvalidPatchException;
import com.florescer.product.domain.exception.custom.ProductNotFoundException;
import com.florescer.product.domain.model.NewProduct;
import com.florescer.product.domain.model.ProductFilter;
import com.florescer.product.domain.model.ProductChanges;
import com.florescer.product.domain.service.ProductService;
import com.florescer.product.infra.repository.ProductRepository;
import com.florescer.product.infra.storage.FileCleanupOnCommit;
import com.florescer.product.infra.logging.SensitiveData;
import com.florescer.product.infra.storage.ImageStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Regras de produto.
 *
 * <p>As operações de escrita registram quem fez, em qual produto e quando. Sem
 * isso não há como responder "quem apagou este produto", que é a primeira
 * pergunta de qualquer auditoria, e a resposta não está em lugar nenhum depois
 * que a linha sai do banco.
 *
 * <p>O autor vem do {@code subject} do token, que neste sistema é o e-mail, e
 * por isso é pseudonimizado antes de ir para o log: auditoria precisa distinguir
 * um autor do outro, não precisa do endereço de ninguém.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Log4j2
public class ProductServiceImpl implements ProductService {

	private final ImageStorageService imageStorageService;
	private final FileCleanupOnCommit fileCleanup;
	private final ProductRepository repository;
	private final SensitiveData sensitiveData;

	@Override
	public Product createProduct(NewProduct command, MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new FileStorageException("Imagem não enviada ou vazia");
		}

		String imagePath = imageStorageService.saveImage(image);

		Product salvo = repository.save(Product.builder()
				.name(command.name())
				.type(command.type())
				.description(command.description())
				.price(command.price())
				.quantityStock(command.quantityStock())
				.careRequirements(command.careRequirements())
				.availability(command.availability())
				.status(command.status())
				.heightCm(command.heightCm())
				.light(command.light())
				.watering(command.watering())
				.petSafe(command.petSafe())
				.environment(command.environment())
				.difficulty(command.difficulty())
				.includesPot(command.includesPot())
				.imagePath(imagePath)
				.build());

		log.info("Produto criado: productId={} autor={}", salvo.getId(), autor());
		return salvo;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> getListProduct(ProductFilter filter, Pageable pageable) {
		return repository.findAll(filter, pageable);
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
		log.info("Produto alterado: productId={} campos={} imagemTrocada={} autor={}",
				id, changes.camposPreenchidos(), !semImagem, autor());
	}

	@Override
	public void deleteProduct(UUID id) {
		Product product = getProductOrThrow(id);
		String imagem = product.getImagePath();

		repository.delete(product);

		// Se o banco desfizer a remoção, o produto continua existindo e precisa
		// da imagem: apagar antes deixaria uma referência apontando para o vazio.
		fileCleanup.deleteAfterCommit(imagem);
		log.info("Produto removido: productId={} autor={}", id, autor());
	}

	/**
	 * Quem está fazendo a operação, sem gravar o endereço em si.
	 *
	 * <p>Devolve "anonimo" quando não há autenticação no contexto, o que na
	 * prática só acontece em chamada interna: as rotas de escrita exigem ADMIN.
	 */
	private String autor() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null) {
			return "anonimo";
		}
		return sensitiveData.pseudonymize(auth.getName());
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
		Optional.ofNullable(changes.heightCm()).ifPresent(product::setHeightCm);
		Optional.ofNullable(changes.light()).ifPresent(product::setLight);
		Optional.ofNullable(changes.watering()).ifPresent(product::setWatering);
		Optional.ofNullable(changes.petSafe()).ifPresent(product::setPetSafe);
		Optional.ofNullable(changes.environment()).ifPresent(product::setEnvironment);
		Optional.ofNullable(changes.difficulty()).ifPresent(product::setDifficulty);
		Optional.ofNullable(changes.includesPot()).ifPresent(product::setIncludesPot);
	}

	private Product getProductOrThrow(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException(id));
	}
}
