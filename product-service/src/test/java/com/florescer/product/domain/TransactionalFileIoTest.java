package com.florescer.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.florescer.product.api.dto.request.ProductCreateRequest;
import com.florescer.product.api.dto.request.ProductPatchRequest;
import com.florescer.product.domain.enums.Status;
import com.florescer.product.domain.exception.personalizadas.FileStorageException;
import com.florescer.product.domain.exception.personalizadas.ProductNotFoundException;
import com.florescer.product.domain.service.ProductService;
import com.florescer.product.infra.repository.ProductRepository;
import com.florescer.product.support.AbstractIntegrationTest;

/**
 * O sistema de arquivos não participa da transação do banco.
 *
 * <p>Apagar um arquivo dentro de uma transação é definitivo mesmo que o banco
 * desfaça tudo depois, o que produzia dois estados incoerentes: o PATCH removia
 * a imagem antiga antes de gravar a nova, e o DELETE apagava o arquivo antes de
 * o banco confirmar.
 */
class TransactionalFileIoTest extends AbstractIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
    private static final byte[] NAO_E_IMAGEM = "<html>nao sou imagem</html>".getBytes();

    @Autowired
    private ProductService service;

    @Autowired
    private ProductRepository repository;

    @Value("${app.uploads.dir}")
    private String uploadDir;

    @Test
    @DisplayName("falha ao gravar a imagem nova preserva a antiga")
    void falhaAoGravarNovaPreservaAAntiga() {
        UUID id = criarProduto();
        String imagemOriginal = repository.findById(id).orElseThrow().getImagePath();
        assertThat(arquivo(imagemOriginal)).exists();

        // Conteúdo que o serviço recusa: simula a gravação falhando no meio.
        MockMultipartFile invalida = new MockMultipartFile(
                "image", "nova.png", MediaType.IMAGE_PNG_VALUE, NAO_E_IMAGEM);

        assertThatThrownBy(() -> service.patchProduct(id, patchVazio(), invalida))
                .isInstanceOf(FileStorageException.class);

        assertThat(arquivo(imagemOriginal))
                .as("a imagem que o produto tinha não pode desaparecer por causa de uma troca que falhou")
                .exists();
        assertThat(repository.findById(id).orElseThrow().getImagePath())
                .as("o produto continua apontando para a imagem original")
                .isEqualTo(imagemOriginal);
    }

    @Test
    @DisplayName("troca bem-sucedida remove a imagem antiga")
    void trocaBemSucedidaRemoveAAntiga() {
        UUID id = criarProduto();
        String imagemOriginal = repository.findById(id).orElseThrow().getImagePath();

        MockMultipartFile nova = new MockMultipartFile(
                "image", "nova.jpg", MediaType.IMAGE_JPEG_VALUE, JPEG);
        service.patchProduct(id, patchVazio(), nova);

        String imagemNova = repository.findById(id).orElseThrow().getImagePath();
        assertThat(imagemNova).isNotEqualTo(imagemOriginal);
        assertThat(arquivo(imagemNova)).exists();
        assertThat(arquivo(imagemOriginal))
                .as("depois do commit, a imagem substituída não precisa mais ocupar espaço")
                .doesNotExist();
    }

    @Test
    @DisplayName("remover o produto remove a imagem")
    void removerProdutoRemoveAImagem() {
        UUID id = criarProduto();
        String imagem = repository.findById(id).orElseThrow().getImagePath();
        assertThat(arquivo(imagem)).exists();

        service.deleteProduct(id);

        assertThat(repository.findById(id)).isEmpty();
        assertThat(arquivo(imagem)).doesNotExist();
    }

    @Test
    @DisplayName("remover produto inexistente nao apaga arquivo algum")
    void removerProdutoInexistenteNaoApagaNada() {
        UUID id = criarProduto();
        String imagem = repository.findById(id).orElseThrow().getImagePath();

        assertThatThrownBy(() -> service.deleteProduct(UUID.randomUUID()))
                .isInstanceOf(ProductNotFoundException.class);

        assertThat(arquivo(imagem))
                .as("uma remoção que falhou não pode levar junto a imagem de outro produto")
                .exists();
    }

    private UUID criarProduto() {
        MockMultipartFile imagem = new MockMultipartFile(
                "image", "original.png", MediaType.IMAGE_PNG_VALUE, PNG);
        return service.createProduct(new ProductCreateRequest(
                "Produto", "Flor", "Descrição", new BigDecimal("10.00"), 5,
                "Regar", true, Status.ATIVO), imagem).id();
    }

    private ProductPatchRequest patchVazio() {
        return new ProductPatchRequest(null, null, null, null, null, null, null, null);
    }

    private Path arquivo(String nome) {
        return Paths.get(uploadDir).resolve(nome);
    }

}
