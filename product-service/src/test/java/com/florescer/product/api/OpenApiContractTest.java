package com.florescer.product.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.product.support.AbstractIntegrationTest;

/**
 * O código trazia um comentário dizendo que o corpo do produto era recebido como
 * texto "temporariamente para poder enviar as imagens pelo swagger". Ao tipar o
 * part, essa justificativa precisa ser verificada em vez de assumida.
 *
 * <p>Estes testes olham o documento OpenAPI gerado, que é o que a interface do
 * Swagger consome para montar o formulário de upload.
 */
@AutoConfigureMockMvc
class OpenApiContractTest extends AbstractIntegrationTest {

    private static final String CREATE_MULTIPART =
            "$.paths['/v1/product'].post.requestBody.content['multipart/form-data'].schema.properties";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("o documento openapi e gerado")
    void documentoEGerado() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
    }

    @Test
    @DisplayName("o formulario de criacao tem as duas partes, produto e imagem")
    void formularioDeCriacaoTemAsDuasPartes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CREATE_MULTIPART + ".product").exists())
                .andExpect(jsonPath(CREATE_MULTIPART + ".image").exists());
    }

    @Test
    @DisplayName("a parte do produto e documentada como objeto, com os campos visiveis")
    void parteDoProdutoEDocumentadaComoObjeto() throws Exception {
        // Se continuasse sendo String, o campo apareceria como type: string e a
        // interface pediria um texto solto, sem mostrar os campos esperados.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CREATE_MULTIPART + ".product.$ref").exists());
    }

    @Test
    @DisplayName("a parte da imagem continua sendo arquivo binario")
    void parteDaImagemEBinaria() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CREATE_MULTIPART + ".image.type").value("string"))
                .andExpect(jsonPath(CREATE_MULTIPART + ".image.format").value("binary"));
    }
}
