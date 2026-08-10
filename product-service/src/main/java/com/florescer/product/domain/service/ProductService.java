package com.florescer.product.domain.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.model.ProductChanges;
import com.florescer.product.domain.model.NewProduct;

/**
 * Operações de catálogo.
 *
 * <p>Fala em termos do próprio domínio: recebe comandos e devolve entidades. Até
 * aqui a interface era escrita em DTOs da API, o que invertia a dependência
 * (domínio conhecendo apresentação) e amarrava as duas: mudar o formato do JSON
 * obrigava a mexer no domínio, e o domínio não servia a nenhum outro consumidor.
 *
 * <p>A conversão para JSON acontece na borda, onde ela pertence.
 */
public interface ProductService {

	Product createProduct(NewProduct command, MultipartFile image);

	Page<Product> getListProduct(Pageable pageable);

	Product getProductById(UUID productId);

	void patchProduct(UUID productId, ProductChanges changes, MultipartFile image);

	void deleteProduct(UUID productId);
}
