package com.florescer.product.infra.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.florescer.product.domain.exception.personalizadas.FileStorageException;

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
            
            Path path = Paths.get(UPLOAD_FOLDER + fileName);

            Files.createDirectories(path.getParent());
            Files.write(path, image.getBytes());

            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
          
            return baseUrl + UPLOAD_FOLDER + fileName;
        } catch (IOException ex) {
        	log.error("Erro ao salvar imagem: {}", ex.getMessage());
            throw new FileStorageException("Erro ao salvar imagem");
        }
    }
}