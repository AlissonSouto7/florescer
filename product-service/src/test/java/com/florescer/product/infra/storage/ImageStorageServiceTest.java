package com.florescer.product.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.florescer.product.domain.exception.personalizadas.FileStorageException;

/**
 * O nome e o tipo de um arquivo enviado são informação que o cliente escolhe.
 *
 * <p>Estes testes tratam os dois como hostis: um nome pode tentar sair da pasta
 * de uploads, e um {@code Content-Type} pode dizer "imagem" sobre qualquer coisa.
 */
class ImageStorageServiceTest {

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    private final ImageStorageService service = new ImageStorageService();

    /** Bytes de arquivos reais, para o serviço ter o que inspecionar. */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
    private static final byte[] HTML = "<script>alert(document.cookie)</script>".getBytes();

    @Test
    @DisplayName("um nome com ../ nao escreve fora da pasta de uploads")
    void nomeComTraversalNaoEscapaDaPastaDeUploads() {
        MockMultipartFile ataque = new MockMultipartFile(
                "image", "../../../../invadido.png", "image/png", PNG);

        String nomeGravado = service.saveImage(ataque);

        Path destino = UPLOAD_DIR.resolve(nomeGravado).normalize().toAbsolutePath();
        assertThat(destino)
                .as("o arquivo precisa terminar dentro da pasta de uploads")
                .startsWithRaw(UPLOAD_DIR.toAbsolutePath().normalize());
        assertThat(nomeGravado)
                .as("o nome gravado nao pode carregar nenhum componente de caminho")
                .doesNotContain("..").doesNotContain("/").doesNotContain("\\");
    }

    @Test
    @DisplayName("o nome escolhido pelo cliente e descartado por completo")
    void nomeOriginalNaoEPreservado() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "image", "foto do cliente (1).png", "image/png", PNG);

        String nomeGravado = service.saveImage(arquivo);

        assertThat(nomeGravado)
                .as("nada do nome original deve sobreviver")
                .doesNotContain("foto").doesNotContain("cliente").doesNotContain(" ")
                .endsWith(".png");
    }

    @Test
    @DisplayName("html disfarcado de imagem pelo content-type e recusado")
    void arquivoQueNaoEImagemEeRecusadoMesmoComContentTypeDeImagem() {
        MockMultipartFile disfarce = new MockMultipartFile(
                "image", "payload.html", "image/png", HTML);

        assertThatThrownBy(() -> service.saveImage(disfarce))
                .as("o conteudo real manda, nao o content-type declarado")
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    @DisplayName("a extensao vem do conteudo real, nao do nome enviado")
    void extensaoDerivaDoConteudoENaoDoNome() {
        MockMultipartFile jpegComNomeErrado = new MockMultipartFile(
                "image", "qualquer.png", "image/jpeg", JPEG);

        String nomeGravado = service.saveImage(jpegComNomeErrado);

        assertThat(nomeGravado)
                .as("bytes de JPEG devem produzir arquivo .jpg, mesmo com nome .png")
                .endsWith(".jpg");
    }

    @Test
    @DisplayName("arquivo vazio e recusado")
    void arquivoVazioERecusado() {
        MockMultipartFile vazio = new MockMultipartFile("image", "vazio.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.saveImage(vazio))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @AfterEach
    void limparArquivosGravados() throws IOException {
        if (!Files.exists(UPLOAD_DIR)) {
            return;
        }
        try (Stream<Path> arquivos = Files.list(UPLOAD_DIR)) {
            arquivos.filter(Files::isRegularFile).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // arquivo de teste; nao vale falhar a suite por causa da limpeza
                }
            });
        }
    }
}
