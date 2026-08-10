package com.florescer.product.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serve as imagens dos produtos.
 *
 * <p>O diretório vem da mesma propriedade usada por {@code ImageStorageService}:
 * se os dois apontassem para lugares diferentes, o upload gravaria num caminho e
 * a leitura procuraria em outro, e o sintoma seria imagem inexistente sem erro
 * algum no log.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public StaticResourceConfig(@Value("${app.uploads.dir:uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path absoluto = Paths.get(uploadDir).toAbsolutePath().normalize();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluto + "/")
                // Cache longo no navegador é seguro porque o nome do arquivo é um
                // UUID: o conteúdo daquele endereço nunca muda, e trocar a imagem
                // de um produto gera outro nome.
                .setCachePeriod(3600)
                // Já a resolução no servidor não pode ser cacheada: o conjunto de
                // arquivos muda a cada upload, e uma imagem recém-enviada
                // continuaria respondendo como inexistente.
                .resourceChain(false);
    }
}
