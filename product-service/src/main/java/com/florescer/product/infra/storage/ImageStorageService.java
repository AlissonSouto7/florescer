package com.florescer.product.infra.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.florescer.product.domain.exception.personalizadas.FileStorageException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ImageStorageService {

    private static final String UPLOAD_FOLDER = "uploads/";

    public String saveImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Imagem não pode ser nula ou vazia.");
        }

        String contentType = image.getContentType();
        if (!List.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
            throw new FileStorageException("Tipo de arquivo não suportado: " + contentType);
        }

        try {
            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path path = Paths.get(UPLOAD_FOLDER, fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, image.getBytes());

            return fileName;
        } catch (IOException ex) {
            log.error("Erro ao salvar imagem: {}", ex.getMessage());
            throw new FileStorageException("Erro ao salvar imagem");
        }
    }

    public static String buildImageUrl(HttpServletRequest request, String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;

        String baseUrl = ServletUriComponentsBuilder
                .fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();

        return baseUrl + "/images/" + imagePath;
    }
    
    public boolean isSameImage(String existingImagePath, MultipartFile newImage) {
        if (existingImagePath == null || newImage == null || newImage.isEmpty()) return false;

        try {
            Path existingPath = Paths.get(UPLOAD_FOLDER, existingImagePath);
            if (!Files.exists(existingPath)) return false;

            byte[] existingBytes = Files.readAllBytes(existingPath);
            byte[] newImageBytes = newImage.getBytes();

            return Arrays.equals(existingBytes, newImageBytes);

        } catch (IOException e) {
            log.warn("Erro ao comparar imagens: {}", e.getMessage());
            return false;
        }
    }
    
    public void deleteOldImageFromDisk(String filePathOrUrl) {
        try {
            if (filePathOrUrl != null && !filePathOrUrl.isBlank()) {
                String fileName;

                if (filePathOrUrl.contains("/")) {
                    // Tenta extrair o nome do arquivo de uma URL ou caminho
                    fileName = filePathOrUrl.substring(filePathOrUrl.lastIndexOf("/") + 1);
                } else {
                    // Já é só o nome do arquivo
                    fileName = filePathOrUrl;
                }

                Path filePath = Paths.get(UPLOAD_FOLDER, fileName);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("Não foi possível excluir a imagem antiga: {}", e.getMessage());
        }
    }
}