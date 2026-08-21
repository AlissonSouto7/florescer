package com.florescer.product.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.product.support.AbstractIntegrationTest;
import com.florescer.product.support.RsaTestKeys;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

/**
 * A vitrine é pública, então a foto do produto precisa ser alcançável por quem
 * ainda não entrou.
 *
 * <p>Havia três problemas somados: a URL era montada apontando para
 * {@code /images/} enquanto o handler estático respondia em {@code /uploads/};
 * a listagem devolvia apenas o nome do arquivo no campo chamado
 * {@code imageUrl}, enquanto o detalhe devolvia a URL completa; e nenhum dos
 * dois caminhos estava liberado para acesso anônimo.
 */
@AutoConfigureMockMvc
class ProductImageUrlTest extends AbstractIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("listagem e detalhe devolvem o mesmo formato de imagem")
    void listagemEDetalheDevolvemOMesmoFormato() throws Exception {
        String id = criarProduto();

        mockMvc.perform(get("/v1/product/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl", containsString("/uploads/")));

        mockMvc.perform(get("/v1/product"))
                .andExpect(status().isOk())
                // O campo tem o mesmo nome nos dois endpoints, então precisa ter
                // o mesmo significado: antes um trazia URL e o outro só o nome.
                .andExpect(jsonPath("$.content[0].imageUrl", containsString("/uploads/")));
    }

    @Test
    @DisplayName("a url da imagem nao aponta para um caminho inexistente")
    void urlNaoApontaParaCaminhoInexistente() throws Exception {
        String id = criarProduto();

        mockMvc.perform(get("/v1/product/{id}", id))
                .andExpect(jsonPath("$.imageUrl", not(containsString("/images/"))));
    }

    @Test
    @DisplayName("visitante anonimo alcanca a imagem")
    void visitanteAnonimoAlcancaAImagem() throws Exception {
        String id = criarProduto();

        // A imagem vem do detalhe deste produto, e não da primeira da listagem:
        // todos os testes compartilham o banco, e outras classes criam produtos
        // com caminho de imagem fictício, sem arquivo no disco. Pegar o primeiro
        // da lista fazia o resultado depender de qual teste rodou antes.
        String corpo = mockMvc.perform(get("/v1/product/{id}", id))
                .andReturn().getResponse().getContentAsString();
        String nomeArquivo = corpo.substring(corpo.indexOf("/uploads/") + "/uploads/".length());
        nomeArquivo = nomeArquivo.substring(0, nomeArquivo.indexOf('"'));

        // Sem token: é assim que o navegador de um visitante busca a foto.
        mockMvc.perform(get("/uploads/{arquivo}", nomeArquivo))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("imagem inexistente devolve 404, nao 500")
    void imagemInexistenteDevolve404() throws Exception {
        // Acontece de verdade quando o arquivo é removido do disco e o registro
        // continua no banco. Responder 500 diria que o servidor tem um defeito.
        mockMvc.perform(get("/uploads/nao-existe.png"))
                .andExpect(status().isNotFound());
    }

    private String criarProduto() throws Exception {
        String json = """
                {"name":"Rosa","type":"Flor","description":"Bonita","price":29.9,
                 "quantityStock":5,"careRequirements":"Regar","availability":true,"status":"ATIVO",
                 "heightCm":40,"light":"MEIA_SOMBRA","watering":"SEMANAL","petSafe":true,
                 "environment":"INTERNO","difficulty":"FACIL","includesPot":true}
                """;

        String corpo = mockMvc.perform(multipart("/v1/product")
                        .file(new MockMultipartFile("product", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes()))
                        .file(new MockMultipartFile("image", "rosa.png", MediaType.IMAGE_PNG_VALUE, PNG))
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return corpo.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    private String tokenAdmin() {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) RsaTestKeys.keyPair().getPublic())
                .privateKey((RSAPrivateKey) RsaTestKeys.keyPair().getPrivate())
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));

        Instant now = Instant.now();
        return encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .issuer("auth-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject("admin@florescer.test")
                .claim("scope", "ADMIN")
                .build())).getTokenValue();
    }
}
