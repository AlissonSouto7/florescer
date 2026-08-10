package com.florescer.product.api.utils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Monta a paginação a partir de parâmetros vindos do cliente.
 *
 * <p>Os três valores chegam pela URL e eram usados como vieram. Isso permitia
 * duas coisas num endpoint público: pedir {@code ?size=1000000}, obrigando o
 * banco e a serialização a materializar tudo de uma vez, e pedir ordenação por
 * qualquer nome, que produzia erro 500 quando o campo não existia na entidade.
 */
public final class PageableFactory {

    /**
     * Campos pelos quais a listagem aceita ordenar.
     *
     * <p>É lista de permissão em vez de bloqueio: qualquer campo novo na
     * entidade nasce indisponível para ordenação até alguém decidir o contrário,
     * em vez de ficar exposto por esquecimento.
     */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("name", "type", "price", "quantityStock", "status", "availability");

    private static final int MAX_PAGE_SIZE = 50;
    private static final String DEFAULT_SORT = "name";

    private PageableFactory() {
    }

    public static Pageable of(int page, int size, String[] sort) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O número da página não pode ser negativo.");
        }
        if (size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O tamanho da página deve ser pelo menos 1.");
        }

        // Reduzir em silêncio seria mentir sobre o que foi devolvido: quem pediu
        // 1000 receberia 50 achando que viu tudo e concluiria que não há mais.
        if (size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O tamanho máximo por página é " + MAX_PAGE_SIZE + ".");
        }

        return PageRequest.of(page, size, buildSort(sort));
    }

    /**
     * Interpreta os parâmetros de ordenação.
     *
     * <p>Um detalhe do Spring exige cuidado: com um parâmetro do tipo
     * {@code String[]}, {@code ?sort=price,desc} chega já separado, como
     * {@code ["price", "desc"]}, e não como um único texto. Tratar cada posição
     * como nome de campo faria a direção ser recusada como campo inválido.
     */
    private static Sort buildSort(String[] sort) {
        if (sort == null || sort.length == 0) {
            return Sort.by(DEFAULT_SORT);
        }

        Set<Sort.Order> orders = new LinkedHashSet<>();
        String campoPendente = null;

        for (String bruto : sort) {
            for (String parte : bruto.split(",")) {
                String valor = parte.trim();
                if (valor.isEmpty()) {
                    continue;
                }

                if (isDirecao(valor)) {
                    if (campoPendente == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "A direção '" + valor + "' precisa vir depois de um campo.");
                    }
                    orders.add("desc".equalsIgnoreCase(valor)
                            ? Sort.Order.desc(campoPendente)
                            : Sort.Order.asc(campoPendente));
                    campoPendente = null;
                    continue;
                }

                if (!SORTABLE_FIELDS.contains(valor)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Não é possível ordenar por '" + valor + "'. Campos disponíveis: "
                                    + String.join(", ", ordenados()));
                }

                // Campo anterior sem direção explícita: sobe como ascendente.
                if (campoPendente != null) {
                    orders.add(Sort.Order.asc(campoPendente));
                }
                campoPendente = valor;
            }
        }

        if (campoPendente != null) {
            orders.add(Sort.Order.asc(campoPendente));
        }

        return orders.isEmpty() ? Sort.by(DEFAULT_SORT) : Sort.by(List.copyOf(orders));
    }

    private static boolean isDirecao(String valor) {
        return "asc".equalsIgnoreCase(valor) || "desc".equalsIgnoreCase(valor);
    }

    /** Lista estável para a mensagem de erro não mudar de ordem a cada chamada. */
    private static List<String> ordenados() {
        String[] campos = SORTABLE_FIELDS.toArray(String[]::new);
        Arrays.sort(campos);
        return List.of(campos);
    }
}
