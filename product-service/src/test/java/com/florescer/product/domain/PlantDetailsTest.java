package com.florescer.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.enums.Difficulty;
import com.florescer.product.domain.enums.Environment;
import com.florescer.product.domain.enums.Light;
import com.florescer.product.domain.enums.Status;
import com.florescer.product.domain.enums.Watering;
import com.florescer.product.infra.repository.ProductRepository;
import com.florescer.product.support.AbstractIntegrationTest;
import com.florescer.product.support.TestTokens;

/**
 * Os campos que quem compra pergunta antes de fechar negócio.
 *
 * <p>Altura, luminosidade e toxicidade para animais não são enfeite de catálogo:
 * são o que evita a conversa de ida e volta antes da venda, e o que evita a
 * planta morrer na casa de quem comprou.
 *
 * <p>Um caso aqui merece atenção especial: planta cadastrada antes destes campos
 * existirem precisa continuar aparecendo na vitrine. As colunas aceitam nulo
 * justamente para não inventar altura nem luminosidade para o que já estava lá.
 */
@AutoConfigureMockMvc
class PlantDetailsTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository repository;

    @Test
    @DisplayName("cadastro guarda e devolve os detalhes da planta")
    void detalhesSaoGuardadosEDevolvidos() throws Exception {
        String corpo = mockMvc.perform(multipart(HttpMethod.POST, "/v1/product")
                        .file(parteDoProduto(completo()))
                        .file(imagem())
                        .header("Authorization", "Bearer " + TestTokens.comEscopo("ADMIN")))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(corpo).get("id").asText();

        JsonNode detalhe = objectMapper.readTree(
                mockMvc.perform(get("/v1/product/{id}", id))
                        .andReturn().getResponse().getContentAsString());

        assertThat(detalhe.get("heightCm").asInt()).as("altura").isEqualTo(40);
        assertThat(detalhe.get("light").asText()).as("luminosidade").isEqualTo("MEIA_SOMBRA");
        assertThat(detalhe.get("watering").asText()).as("rega").isEqualTo("SEMANAL");
        assertThat(detalhe.get("petSafe").asBoolean()).as("segura para animais").isTrue();
        assertThat(detalhe.get("environment").asText()).as("ambiente").isEqualTo("INTERNO");
        assertThat(detalhe.get("difficulty").asText()).as("dificuldade").isEqualTo("FACIL");
        assertThat(detalhe.get("includesPot").asBoolean()).as("vem com vaso").isTrue();
    }

    @Test
    @DisplayName("a listagem tambem traz os detalhes, para a vitrine filtrar")
    void listagemTrazOsDetalhes() throws Exception {
        String corpo = mockMvc.perform(multipart(HttpMethod.POST, "/v1/product")
                        .file(parteDoProduto(completo()))
                        .file(imagem())
                        .header("Authorization", "Bearer " + TestTokens.comEscopo("ADMIN")))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpo).get("id").asText();

        JsonNode pagina = objectMapper.readTree(
                mockMvc.perform(get("/v1/product?size=50"))
                        .andReturn().getResponse().getContentAsString());

        // Procura pelo id em vez de pegar o primeiro da página: o banco é
        // compartilhado pela suíte e a ordenação é por nome, então o primeiro
        // item pode ser de outro teste. Passaria por sorte, e falharia quando a
        // ordem de execução mudasse, que é o pior tipo de teste quebrado.
        JsonNode item = null;
        for (JsonNode candidato : pagina.get("content")) {
            if (id.equals(candidato.get("id").asText())) {
                item = candidato;
                break;
            }
        }
        assertThat(item).as("a planta recém-criada precisa aparecer na listagem").isNotNull();

        // Se a listagem não trouxer, a vitrine precisaria de uma requisição por
        // planta só para saber se ela é segura para gato.
        assertThat(item.has("heightCm")).as("listagem precisa expor altura").isTrue();
        assertThat(item.has("light")).as("listagem precisa expor luminosidade").isTrue();
        assertThat(item.has("petSafe")).as("listagem precisa expor segurança para animais").isTrue();
    }

    @Test
    @DisplayName("cadastro sem os detalhes e recusado, campo a campo")
    void cadastroIncompletoERecusado() throws Exception {
        String semDetalhes = """
                {"name":"Samambaia","type":"Planta","description":"Verde e vicosa",
                 "price":49.90,"quantityStock":3,"careRequirements":"Meia sombra",
                 "availability":true,"status":"ATIVO"}
                """;

        String resposta = mockMvc.perform(multipart(HttpMethod.POST, "/v1/product")
                        .file(parteDoProduto(semDetalhes))
                        .file(imagem())
                        .header("Authorization", "Bearer " + TestTokens.comEscopo("ADMIN")))
                .andReturn().getResponse().getContentAsString();

        // A resposta nomeia cada campo faltando, senão quem cadastra recebe
        // "erro de validação" e precisa adivinhar o que esqueceu.
        assertThat(resposta)
                .contains("heightCm")
                .contains("light")
                .contains("watering")
                .contains("petSafe")
                .contains("environment")
                .contains("difficulty")
                .contains("includesPot");
    }

    @Test
    @DisplayName("valor fora do enum e recusado com 400, nao com erro de servidor")
    void enumInvalidoERecusado() throws Exception {
        String luzInexistente = completo().replace("\"light\":\"MEIA_SOMBRA\"", "\"light\":\"PENUMBRA\"");

        int status = mockMvc.perform(multipart(HttpMethod.POST, "/v1/product")
                        .file(parteDoProduto(luzInexistente))
                        .file(imagem())
                        .header("Authorization", "Bearer " + TestTokens.comEscopo("ADMIN")))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("enum desconhecido é entrada inválida do cliente, não defeito do servidor")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("altura absurda e recusada")
    void alturaForaDoLimiteERecusada() throws Exception {
        // 50 metros é erro de digitação, não uma planta em vaso.
        String alturaAbsurda = completo().replace("\"heightCm\":40", "\"heightCm\":5000");

        int status = mockMvc.perform(multipart(HttpMethod.POST, "/v1/product")
                        .file(parteDoProduto(alturaAbsurda))
                        .file(imagem())
                        .header("Authorization", "Bearer " + TestTokens.comEscopo("ADMIN")))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(400);
    }

    @Test
    @DisplayName("planta cadastrada antes destes campos continua na vitrine")
    void plantaAntigaSemDetalhesContinuaListada() throws Exception {
        // Grava direto pelo repositório, como estão as linhas que existiam antes
        // da migration. É por isso que as colunas aceitam nulo: inventar altura
        // para elas colocaria informação falsa na vitrine.
        Product antiga = repository.save(Product.builder()
                .name("Jiboia antiga")
                .type("Pendente")
                .description("Cadastrada antes dos detalhes existirem")
                .price(new BigDecimal("30.00"))
                .quantityStock(1)
                .careRequirements("Regar quando o substrato secar")
                .availability(true)
                .status(Status.ATIVO)
                .imagePath("imagens/antiga.png")
                .build());

        JsonNode detalhe = objectMapper.readTree(
                mockMvc.perform(get("/v1/product/{id}", antiga.getId()))
                        .andReturn().getResponse().getContentAsString());

        assertThat(detalhe.get("name").asText()).isEqualTo("Jiboia antiga");
        assertThat(detalhe.get("heightCm").isNull())
                .as("campo ausente vem nulo, e não com um valor inventado")
                .isTrue();

        int status = mockMvc.perform(get("/v1/product?size=50")).andReturn().getResponse().getStatus();
        assertThat(status).as("a listagem não pode quebrar por causa dela").isEqualTo(200);
    }

    @Test
    @DisplayName("patch altera so o detalhe enviado")
    void patchAlteraApenasODetalheEnviado() throws Exception {
        String corpo = mockMvc.perform(multipart(HttpMethod.POST, "/v1/product")
                        .file(parteDoProduto(completo()))
                        .file(imagem())
                        .header("Authorization", "Bearer " + TestTokens.comEscopo("ADMIN")))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpo).get("id").asText();

        mockMvc.perform(multipart(HttpMethod.PATCH, "/v1/product/{id}", id)
                .file(parteDoProduto("{\"heightCm\":80}"))
                .header("Authorization", "Bearer " + TestTokens.comEscopo("ADMIN")));

        JsonNode depois = objectMapper.readTree(
                mockMvc.perform(get("/v1/product/{id}", id))
                        .andReturn().getResponse().getContentAsString());

        assertThat(depois.get("heightCm").asInt()).as("o campo enviado muda").isEqualTo(80);
        assertThat(depois.get("light").asText()).as("os outros ficam").isEqualTo("MEIA_SOMBRA");
        assertThat(depois.get("petSafe").asBoolean()).as("os outros ficam").isTrue();
    }

    private String completo() {
        return """
                {"name":"Samambaia","type":"Planta","description":"Verde e vicosa",
                 "price":49.90,"quantityStock":3,"careRequirements":"Meia sombra",
                 "availability":true,"status":"ATIVO",
                 "heightCm":40,"light":"MEIA_SOMBRA","watering":"SEMANAL",
                 "petSafe":true,"environment":"INTERNO","difficulty":"FACIL","includesPot":true}
                """;
    }

    private MockMultipartFile parteDoProduto(String json) {
        return new MockMultipartFile("product", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile imagem() {
        return new MockMultipartFile("image", "planta.png", MediaType.IMAGE_PNG_VALUE,
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk"
                                + "YPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="));
    }
}
