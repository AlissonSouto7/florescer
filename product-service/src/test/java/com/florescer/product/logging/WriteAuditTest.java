package com.florescer.product.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
import com.florescer.product.support.AbstractIntegrationTest;
import com.florescer.product.support.LogCapture;
import com.florescer.product.support.TestTokens;

/**
 * Toda escrita deixa rastro de quem fez e em quê.
 *
 * <p>Antes disso o serviço só falava quando dava errado: criar, alterar e apagar
 * produto não geravam nenhuma linha. "Quem apagou este produto e quando" é a
 * primeira pergunta de qualquer auditoria, e a resposta não existia em lugar
 * nenhum depois que a linha saía do banco.
 *
 * <p>O autor entra pseudonimizado. Auditoria precisa distinguir um autor do
 * outro, não precisa do e-mail de ninguém, e o log tem retenção mais longa e
 * acesso mais amplo que o banco.
 */
@AutoConfigureMockMvc
class WriteAuditTest extends AbstractIntegrationTest {

    private static final String EMAIL_DO_ADMIN = "pessoa@exemplo.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("criacao registra o produto e o autor")
    void criacaoRegistraAutorEAlvo() throws Exception {
        try (LogCapture log = LogCapture.start()) {
            String id = criarProduto();

            assertThat(log.all())
                    .as("a linha precisa dizer qual produto e quem criou")
                    .contains("Produto criado")
                    .contains(id)
                    .contains("autor=");
        }
    }

    @Test
    @DisplayName("remocao registra o produto e o autor")
    void remocaoRegistraAutorEAlvo() throws Exception {
        String id = criarProduto();

        try (LogCapture log = LogCapture.start()) {
            mockMvc.perform(delete("/v1/product/{id}", id)
                    .header("Authorization", "Bearer " + TestTokens.comEscopo("ADMIN")));

            assertThat(log.all())
                    .as("apagar é a operação mais destrutiva e a que mais precisa de rastro")
                    .contains("Produto removido")
                    .contains(id);
        }
    }

    @Test
    @DisplayName("o e-mail do autor nao aparece em texto puro")
    void autorEPseudonimizado() throws Exception {
        try (LogCapture log = LogCapture.start()) {
            criarProduto();

            assertThat(log.all())
                    .as("registrar o autor não pode virar uma segunda base de dados pessoais")
                    .doesNotContain(EMAIL_DO_ADMIN);
        }
    }

    private String criarProduto() throws Exception {
        String corpo = mockMvc.perform(multipart("/v1/product")
                        .file(new MockMultipartFile("product", "", MediaType.APPLICATION_JSON_VALUE,
                                """
                                {"name":"Samambaia","type":"Planta","description":"Verde e viçosa",
                                 "price":49.90,"quantityStock":3,"careRequirements":"Meia sombra",
                                 "availability":true,"status":"ATIVO",
                 "heightCm":40,"light":"MEIA_SOMBRA","watering":"SEMANAL","petSafe":true,
                 "environment":"INTERNO","difficulty":"FACIL","includesPot":true}
                                """.getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("image", "planta.png", MediaType.IMAGE_PNG_VALUE, pngMinimo()))
                        .header("Authorization", "Bearer " + TestTokens.comEscopo("ADMIN")))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(corpo).get("id").asText();
    }

    private static byte[] pngMinimo() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk"
                        + "YPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
    }
}
