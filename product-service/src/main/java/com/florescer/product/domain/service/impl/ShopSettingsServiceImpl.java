package com.florescer.product.domain.service.impl;

import java.time.Instant;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.florescer.product.domain.entity.ShopSettings;
import com.florescer.product.domain.exception.custom.InvalidSettingsException;
import com.florescer.product.domain.model.ShopSettingsChanges;
import com.florescer.product.domain.service.ShopSettingsService;
import com.florescer.product.infra.repository.ShopSettingsJPARepository;

import lombok.RequiredArgsConstructor;

/**
 * Guarda os dados da loja, deixando-os utilizáveis antes de gravar.
 *
 * <p>A limpeza mora aqui, e não na tela, por um motivo prático: quem chama a API
 * direto não passa pela tela. Se a normalização vivesse no formulário, um
 * {@code curl} gravaria "@loja" e o rodapé montaria
 * {@code instagram.com/@loja}, que não abre o perfil de ninguém.
 */
@Service
@RequiredArgsConstructor
public class ShopSettingsServiceImpl implements ShopSettingsService {

    private final ShopSettingsJPARepository repository;

    /** Tudo que não é dígito sai do telefone. */
    private static final Pattern NAO_DIGITO = Pattern.compile("\\D");

    /** Um fixo com DDD tem 10 dígitos; 15 é o teto do padrão E.164. */
    private static final int MINIMO_DE_DIGITOS = 10;
    private static final int MAXIMO_DE_DIGITOS = 15;

    /**
     * O que descartar antes de sobrar o @ do Instagram.
     *
     * <p>As pessoas escrevem de cinco jeitos: {@code @loja}, {@code loja},
     * {@code instagram.com/loja}, {@code https://www.instagram.com/loja} e com
     * barra no fim. Todos precisam virar {@code loja}, senão o link do rodapé
     * aponta para uma página que não existe.
     */
    private static final Pattern ENFEITE_DO_INSTAGRAM =
            Pattern.compile("^(https?://)?(www\\.)?(instagram\\.com/)?@?|/.*$");

    @Override
    @Transactional(readOnly = true)
    public ShopSettings obter() {
        return repository.findById(ShopSettings.ID_UNICO)
                // Não é "não encontrado" para quem chama: a linha nasce com a
                // migration V3, então a ausência dela significa banco fora do
                // esperado, e não um caminho normal da aplicação.
                .orElseThrow(() -> new IllegalStateException(
                        "A linha de configuração da loja não existe. A migration V3 deveria tê-la criado."));
    }

    @Override
    @Transactional
    public ShopSettings salvar(ShopSettingsChanges mudancas) {
        ShopSettings atual = obter();

        atual.setWhatsappNumber(somenteDigitos(mudancas.whatsappNumber()));
        atual.setDeliveryCity(limpar(mudancas.deliveryCity()));
        atual.setInstagramHandle(apenasOPerfil(mudancas.instagramHandle()));
        atual.setOpeningHours(limpar(mudancas.openingHours()));
        atual.setUpdatedAt(Instant.now());

        return repository.save(atual);
    }

    /**
     * Texto sem espaço sobrando, e vazio vira nulo.
     *
     * <p>A diferença importa na hora de exibir: nulo significa "não tenho isso"
     * e a linha some do rodapé, enquanto string vazia deixaria um rótulo
     * pendurado sem valor nenhum ao lado.
     */
    private String limpar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    /**
     * O telefone como o wa.me exige: só dígitos, e só se der para ligar.
     *
     * <p>A validação acontece <b>aqui</b>, e não numa anotação do DTO, porque
     * ela precisa olhar o número já sem pontuação. "+55 (11) 98765-4321" tem 19
     * caracteres e 13 dígitos: recusá-lo por causa dos parênteses seria obrigar
     * a pessoa a digitar do jeito que a máquina prefere.
     */
    private String somenteDigitos(String valor) {
        String limpo = limpar(valor);
        if (limpo == null) {
            return null;
        }

        String digitos = NAO_DIGITO.matcher(limpo).replaceAll("");
        if (digitos.isEmpty()) {
            // Apagar o número é permitido: pode ser que ela ainda não tenha um,
            // e nesse caso o botão de comprar some, o que é melhor que um botão
            // levando a lugar nenhum.
            return null;
        }

        if (digitos.length() < MINIMO_DE_DIGITOS || digitos.length() > MAXIMO_DE_DIGITOS) {
            throw new InvalidSettingsException("whatsappNumber",
                    "Digite o número com o código do país e o DDD. Exemplo: 5511987654321");
        }

        return digitos;
    }

    /** O @ do perfil, venha ele como vier. */
    private String apenasOPerfil(String valor) {
        String limpo = limpar(valor);
        if (limpo == null) {
            return null;
        }
        String perfil = ENFEITE_DO_INSTAGRAM.matcher(limpo).replaceAll("");
        return perfil.isEmpty() ? null : perfil;
    }
}
