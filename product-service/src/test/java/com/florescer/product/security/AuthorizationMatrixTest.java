package com.florescer.product.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.florescer.product.support.AbstractIntegrationTest;
import com.florescer.product.support.TestTokens;

/**
 * Percorre cada endpoint contra cada perfil.
 *
 * <p>Autorização quebra em silêncio. Uma anotação removida por engano não faz
 * nenhum outro teste falhar: o endpoint continua respondendo, só que para quem
 * não devia. Esta matriz existe para que essa remoção passe a ter consequência.
 *
 * <p>A distinção entre 401 e 403 é parte do que está sob teste. Quem não se
 * identificou precisa receber 401, porque a ação a tomar é autenticar. Quem se
 * identificou e não tem o papel recebe 403, porque autenticar de novo não muda
 * nada.
 */
@AutoConfigureMockMvc
class AuthorizationMatrixTest extends AbstractIntegrationTest {

    private static final String COLECAO = "/v1/product";
    private static final String ITEM = "/v1/product/" + UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    /**
     * O esperado para cada combinação.
     *
     * <p>Nas rotas de escrita, o perfil ADMIN não espera 2xx: o produto sorteado
     * não existe e o corpo é mínimo, então a resposta correta é 404 ou 400. O que
     * importa é que não seja 401 nem 403, porque é isso que separa "foi
     * autorizado" de "foi barrado na porta".
     */
    private record Caso(String descricao, HttpMethod metodo, String rota, Perfil perfil, int esperado) {
        @Override
        public String toString() {
            return descricao;
        }
    }

    private enum Perfil {
        ANONIMO, BASIC, ADMIN
    }

    static List<Caso> matriz() {
        return List.of(
                // Leitura é pública: a vitrine precisa funcionar para quem ainda
                // não tem conta.
                new Caso("GET coleção, anônimo -> 200", HttpMethod.GET, COLECAO, Perfil.ANONIMO, 200),
                new Caso("GET coleção, BASIC -> 200", HttpMethod.GET, COLECAO, Perfil.BASIC, 200),
                new Caso("GET coleção, ADMIN -> 200", HttpMethod.GET, COLECAO, Perfil.ADMIN, 200),

                new Caso("GET item, anônimo -> 404", HttpMethod.GET, ITEM, Perfil.ANONIMO, 404),
                new Caso("GET item, BASIC -> 404", HttpMethod.GET, ITEM, Perfil.BASIC, 404),

                // Escrita é de ADMIN.
                new Caso("POST, anônimo -> 401", HttpMethod.POST, COLECAO, Perfil.ANONIMO, 401),
                new Caso("POST, BASIC -> 403", HttpMethod.POST, COLECAO, Perfil.BASIC, 403),

                new Caso("PATCH, anônimo -> 401", HttpMethod.PATCH, ITEM, Perfil.ANONIMO, 401),
                new Caso("PATCH, BASIC -> 403", HttpMethod.PATCH, ITEM, Perfil.BASIC, 403),

                new Caso("DELETE, anônimo -> 401", HttpMethod.DELETE, ITEM, Perfil.ANONIMO, 401),
                new Caso("DELETE, BASIC -> 403", HttpMethod.DELETE, ITEM, Perfil.BASIC, 403),
                new Caso("DELETE, ADMIN -> 404 (autorizado, produto ausente)",
                        HttpMethod.DELETE, ITEM, Perfil.ADMIN, 404));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matriz")
    void perfilAlcancaApenasOQueDeve(Caso caso) throws Exception {
        int status = mockMvc.perform(requisicao(caso)).andReturn().getResponse().getStatus();

        assertThat(status)
                .as("%s %s como %s", caso.metodo(), caso.rota(), caso.perfil())
                .isEqualTo(caso.esperado());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("escritaComoAdmin")
    @DisplayName("ADMIN passa da autorizacao nas rotas de escrita")
    void adminNaoEBarrado(Caso caso) throws Exception {
        int status = mockMvc.perform(requisicao(caso)).andReturn().getResponse().getStatus();

        // Verificado pela negativa de propósito: fixar o código exato aqui
        // amarraria a matriz a detalhes de validação de corpo, que mudam por
        // outros motivos e fariam este teste falhar sem nada de errado com a
        // autorização.
        assertThat(status)
                .as("%s %s como ADMIN não pode ser barrado por autorização", caso.metodo(), caso.rota())
                .isNotIn(401, 403);
    }

    static List<Caso> escritaComoAdmin() {
        return List.of(
                new Caso("POST como ADMIN", HttpMethod.POST, COLECAO, Perfil.ADMIN, 0),
                new Caso("PATCH como ADMIN", HttpMethod.PATCH, ITEM, Perfil.ADMIN, 0));
    }

    /**
     * O tipo do construtor mudou no Spring Framework 7.
     *
     * <p>{@code MockMultipartHttpServletRequestBuilder} deixou de ser um
     * {@code MockHttpServletRequestBuilder}: os dois passaram a descender de
     * {@code AbstractMockHttpServletRequestBuilder}, que é o tipo comum onde
     * {@code header()} ainda existe. Sem isso, misturar GET e multipart no
     * mesmo switch não compila.
     */
    private AbstractMockHttpServletRequestBuilder<?> requisicao(Caso caso) {
        AbstractMockHttpServletRequestBuilder<?> builder = switch (caso.metodo().name()) {
            case "GET" -> MockMvcRequestBuilders.get(caso.rota());
            case "DELETE" -> MockMvcRequestBuilders.delete(caso.rota());
            case "POST" -> multipart(HttpMethod.POST, caso.rota());
            case "PATCH" -> multipart(HttpMethod.PATCH, caso.rota());
            default -> throw new IllegalArgumentException("Metodo nao previsto: " + caso.metodo());
        };

        return switch (caso.perfil()) {
            case ANONIMO -> builder;
            case BASIC -> builder.header("Authorization", "Bearer " + TestTokens.comEscopo("BASIC"));
            case ADMIN -> builder.header("Authorization", "Bearer " + TestTokens.comEscopo("ADMIN"));
        };
    }

    private MockMultipartHttpServletRequestBuilder multipart(HttpMethod metodo, String rota) {
        return MockMvcRequestBuilders.multipart(metodo, rota)
                .file(new MockMultipartFile("product", "", MediaType.APPLICATION_JSON_VALUE,
                        """
                        {"name":"Samambaia","type":"Planta","description":"Verde e viçosa",
                         "price":49.90,"quantityStock":3,"careRequirements":"Meia sombra",
                         "availability":true,"status":"ATIVO",
                 "heightCm":40,"light":"MEIA_SOMBRA","watering":"SEMANAL","petSafe":true,
                 "environment":"INTERNO","difficulty":"FACIL","includesPot":true}
                        """.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("image", "planta.png", MediaType.IMAGE_PNG_VALUE, pngMinimo()));
    }

    private static byte[] pngMinimo() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk"
                        + "YPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
    }
}
