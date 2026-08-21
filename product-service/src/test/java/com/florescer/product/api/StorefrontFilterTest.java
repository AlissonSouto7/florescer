package com.florescer.product.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.enums.Difficulty;
import com.florescer.product.domain.enums.Environment;
import com.florescer.product.domain.enums.Light;
import com.florescer.product.domain.enums.Status;
import com.florescer.product.domain.enums.Watering;
import com.florescer.product.infra.repository.ProductJPARepository;
import com.florescer.product.infra.repository.ProductRepository;
import com.florescer.product.support.AbstractIntegrationTest;

/**
 * Os filtros da vitrine.
 *
 * <p>Sem eles, quem tem gato precisa abrir planta por planta para descobrir
 * quais são seguras, e quem mora em apartamento sem sol se apaixona por uma
 * planta que vai morrer na casa dele.
 *
 * <p>O filtro roda no banco. Mandar o catálogo inteiro para o navegador filtrar
 * funciona com dez plantas e piora a cada planta nova, gastando banda de quem
 * está no celular.
 */
@AutoConfigureMockMvc
class StorefrontFilterTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository repository;

    @Autowired
    private ProductJPARepository jpaRepository;

    @BeforeEach
    void montarCatalogo() {
        // O contexto e o banco são compartilhados por toda a suíte, e nada aqui
        // desfaz o que foi gravado. Sem limpar, o catálogo do caso anterior soma
        // ao deste e as contagens deixam de significar o que dizem: a primeira
        // execução acusou 18 plantas onde este teste cria 6.
        jpaRepository.deleteAll();

        planta("Samambaia", Light.MEIA_SOMBRA, true, Environment.INTERNO, Difficulty.FACIL, "49.90", 3, true);
        planta("Cacto", Light.SOL_PLENO, true, Environment.EXTERNO, Difficulty.FACIL, "25.00", 5, true);
        planta("Comigo-ninguem-pode", Light.SOMBRA, false, Environment.INTERNO, Difficulty.MEDIO, "80.00", 2, true);
        planta("Jiboia", Light.SOMBRA, false, Environment.AMBOS, Difficulty.FACIL, "35.00", 4, true);
        planta("Orquidea rara", Light.MEIA_SOMBRA, true, Environment.INTERNO, Difficulty.DIFICIL, "250.00", 1, true);
        // Esgotada: existe no catálogo, mas não há o que vender.
        planta("Suculenta esgotada", Light.SOL_PLENO, true, Environment.AMBOS, Difficulty.FACIL, "15.00", 0, false);
    }

    @Test
    @DisplayName("sem filtro devolve o catalogo inteiro")
    void semFiltroDevolveTudo() throws Exception {
        assertThat(nomesDe("")).hasSize(6);
    }

    @Test
    @DisplayName("filtra por seguranca para animais")
    void filtraPorSegurancaParaAnimais() throws Exception {
        List<String> seguras = nomesDe("&petSafe=true");

        assertThat(seguras)
                .as("quem tem gato precisa ver só as seguras")
                .doesNotContain("Comigo-ninguem-pode", "Jiboia")
                .contains("Samambaia", "Cacto");
    }

    @Test
    @DisplayName("filtra por luminosidade")
    void filtraPorLuminosidade() throws Exception {
        assertThat(nomesDe("&light=SOMBRA"))
                .containsExactlyInAnyOrder("Comigo-ninguem-pode", "Jiboia");
    }

    @Test
    @DisplayName("planta de ambiente AMBOS aparece nas duas buscas")
    void ambosApareceNasDuasBuscas() throws Exception {
        // A Jiboia vai bem dentro e fora. Se o filtro comparasse só por igualdade,
        // ela sumiria das duas buscas e a vendedora perderia a venda sem saber
        // por quê.
        assertThat(nomesDe("&environment=INTERNO"))
                .as("busca por interno precisa incluir a que serve para os dois")
                .contains("Jiboia", "Samambaia");

        assertThat(nomesDe("&environment=EXTERNO"))
                .as("e a busca por externo também")
                .contains("Jiboia", "Cacto");
    }

    @Test
    @DisplayName("filtra por teto de preco")
    void filtraPorTetoDePreco() throws Exception {
        List<String> ateCinquenta = nomesDe("&maxPrice=50.00");

        assertThat(ateCinquenta)
                .as("quem tem um teto não quer se apaixonar pelo que não vai comprar")
                .contains("Samambaia", "Cacto", "Jiboia")
                .doesNotContain("Orquidea rara", "Comigo-ninguem-pode");
    }

    @Test
    @DisplayName("onlyAvailable esconde o que nao ha para vender")
    void escondeOEsgotado() throws Exception {
        assertThat(nomesDe("&onlyAvailable=true"))
                .as("planta sem estoque só gera pedido que a vendedora não pode atender")
                .doesNotContain("Suculenta esgotada")
                .hasSize(5);
    }

    @Test
    @DisplayName("filtros se combinam com E")
    void filtrosSeCombinam() throws Exception {
        List<String> resultado = nomesDe("&petSafe=true&difficulty=FACIL&maxPrice=50.00&onlyAvailable=true");

        // Segura para pets, fácil de cuidar, até R$ 50 e disponível.
        assertThat(resultado).containsExactlyInAnyOrder("Samambaia", "Cacto");
    }

    @Test
    @DisplayName("filtro sem resultado devolve pagina vazia, nao erro")
    void filtroSemResultado() throws Exception {
        assertThat(nomesDe("&light=SOL_PLENO&environment=INTERNO&maxPrice=1.00")).isEmpty();
    }

    @Test
    @DisplayName("valor invalido no filtro responde 400, nao erro de servidor")
    void valorInvalidoNoFiltro() throws Exception {
        int status = mockMvc.perform(get("/v1/product?size=50&light=PENUMBRA"))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("enum desconhecido é entrada inválida do cliente")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("a vitrine filtrada continua publica")
    void filtroNaoExigeAutenticacao() throws Exception {
        // Sem token: é a vitrine que qualquer pessoa navega.
        int status = mockMvc.perform(get("/v1/product?size=50&petSafe=true"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(200);
    }

    private List<String> nomesDe(String filtros) throws Exception {
        String corpo = mockMvc.perform(get("/v1/product?size=50" + filtros))
                .andReturn().getResponse().getContentAsString();

        List<String> nomes = new ArrayList<>();
        for (JsonNode item : objectMapper.readTree(corpo).get("content")) {
            nomes.add(item.get("name").asText());
        }
        return nomes;
    }

    private void planta(String nome, Light luz, boolean seguraParaPets, Environment ambiente,
            Difficulty dificuldade, String preco, int estoque, boolean disponivel) {
        repository.save(Product.builder()
                .name(nome)
                .type("Planta")
                .description("Planta do catálogo de teste")
                .price(new BigDecimal(preco))
                .quantityStock(estoque)
                .careRequirements("Cuidados padrão")
                .availability(disponivel)
                .status(Status.ATIVO)
                .heightCm(40)
                .light(luz)
                .watering(Watering.SEMANAL)
                .petSafe(seguraParaPets)
                .environment(ambiente)
                .difficulty(dificuldade)
                .includesPot(true)
                .imagePath("imagens/" + nome.toLowerCase().replace(' ', '-') + ".png")
                .build());
    }
}
