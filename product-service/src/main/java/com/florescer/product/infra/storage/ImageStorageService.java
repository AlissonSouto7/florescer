package com.florescer.product.infra.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.florescer.product.domain.exception.personalizadas.FileStorageException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

/**
 * Grava as imagens dos produtos em disco.
 *
 * <p>Duas informações que acompanham um upload vêm do cliente e não podem ser
 * acreditadas: o nome do arquivo e o {@code Content-Type}. O nome pode conter
 * componentes de caminho e levar a escrita para fora da pasta; o tipo declarado
 * pode dizer "imagem" sobre qualquer conteúdo.
 *
 * <p>Por isso o nome original é descartado inteiro, e o tipo é decidido pelos
 * primeiros bytes do arquivo, que são o único dado que o cliente não escolhe
 * sem de fato enviar aquele conteúdo.
 */
@Component
@Log4j2
public class ImageStorageService {

    private static final String UPLOAD_FOLDER = "uploads";

    /** Assinaturas dos formatos aceitos, na ordem em que aparecem no arquivo. */
    private enum ImageType {
        JPEG(".jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        PNG(".png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
        WEBP(".webp", new byte[]{'R', 'I', 'F', 'F'});

        private final String extension;
        private final byte[] magic;

        ImageType(String extension, byte[] magic) {
            this.extension = extension;
            this.magic = magic;
        }

        boolean matches(byte[] header) {
            if (header.length < magic.length) {
                return false;
            }
            return Arrays.equals(header, 0, magic.length, magic, 0, magic.length);
        }
    }

    public String saveImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Imagem não pode ser nula ou vazia.");
        }

        try {
            byte[] content = image.getBytes();
            ImageType type = detectType(content);

            // O nome do cliente é descartado por inteiro. Nada que ele escolheu
            // chega ao sistema de arquivos: nem nome, nem extensão, nem separador.
            String fileName = UUID.randomUUID() + type.extension;

            Path uploadDir = uploadDirectory();
            Path destination = uploadDir.resolve(fileName).normalize();

            // O nome é gerado aqui e não teria como escapar, mas a verificação
            // fica como rede: se a forma de gerar mudar, o teste e esta guarda
            // continuam valendo.
            if (!destination.startsWith(uploadDir)) {
                throw new FileStorageException("Caminho de destino inválido.");
            }

            Files.createDirectories(uploadDir);
            Files.write(destination, content);

            return fileName;
        } catch (IOException ex) {
            log.error("Erro ao salvar imagem: {}", ex.getMessage());
            throw new FileStorageException("Erro ao salvar imagem");
        }
    }

    public static String buildImageUrl(HttpServletRequest request, String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String baseUrl = ServletUriComponentsBuilder
                .fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();

        return baseUrl + "/images/" + imagePath;
    }

    /**
     * Compara pelo hash em vez de carregar os dois arquivos inteiros na memória.
     * Com uploads de até 10 MB e requisições concorrentes, ler tudo de uma vez
     * multiplica o consumo por requisição sem necessidade.
     */
    public boolean isSameImage(String existingImagePath, MultipartFile newImage) {
        if (existingImagePath == null || newImage == null || newImage.isEmpty()) {
            return false;
        }

        try {
            Path existingPath = resolveInsideUploads(existingImagePath);
            if (existingPath == null || !Files.exists(existingPath)) {
                return false;
            }

            return digest(Files.newInputStream(existingPath)).equals(digest(newImage.getInputStream()));
        } catch (IOException e) {
            log.warn("Erro ao comparar imagens: {}", e.getMessage());
            return false;
        }
    }

    public void deleteOldImageFromDisk(String filePathOrUrl) {
        try {
            Path filePath = resolveInsideUploads(filePathOrUrl);
            if (filePath != null) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("Não foi possível excluir a imagem antiga: {}", e.getMessage());
        }
    }

    private ImageType detectType(byte[] content) {
        return Arrays.stream(ImageType.values())
                .filter(type -> type.matches(content))
                .findFirst()
                .orElseThrow(() -> new FileStorageException(
                        "Arquivo não é uma imagem JPEG, PNG ou WebP válida."));
    }

    /**
     * Resolve um nome de arquivo dentro da pasta de uploads, recusando qualquer
     * valor que tente sair dela. Aceita tanto o nome puro quanto uma URL, já que
     * o valor pode chegar dos dois jeitos.
     */
    private Path resolveInsideUploads(String filePathOrUrl) {
        if (filePathOrUrl == null || filePathOrUrl.isBlank()) {
            return null;
        }

        // Trata / e \ porque o serviço roda tanto em Linux quanto em Windows.
        String fileName = filePathOrUrl.replace('\\', '/');
        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }

        if (fileName.isBlank() || fileName.contains("..")) {
            return null;
        }

        Path uploadDir = uploadDirectory();
        Path resolved = uploadDir.resolve(fileName).normalize();
        return resolved.startsWith(uploadDir) ? resolved : null;
    }

    private Path uploadDirectory() {
        return Paths.get(UPLOAD_FOLDER).toAbsolutePath().normalize();
    }

    private String digest(InputStream stream) throws IOException {
        try (stream) {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                sha256.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(sha256.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
