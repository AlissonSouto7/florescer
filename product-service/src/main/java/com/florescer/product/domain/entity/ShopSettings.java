package com.florescer.product.domain.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Os dados da loja que a vendedora troca sozinha.
 *
 * <p>Antes o número do WhatsApp vinha de variável de ambiente e o rodapé tinha
 * texto fixo no código. Trocar qualquer um exigia editar arquivo e reiniciar
 * container, o que ela não faz, e um número desatualizado é o pior defeito
 * possível aqui: o pedido não chega em ninguém e nada na tela indica isso.
 *
 * <p><b>Uma linha só.</b> A configuração é de uma loja, não de várias, e o
 * identificador fixo em 1 com CHECK no banco é o que garante isso. Sem essa
 * trava, um bug criaria uma segunda linha e passaria a existir a pergunta "qual
 * das duas vale?", sem resposta possível na leitura.
 *
 * <p>Todos os campos aceitam nulo: loja recém-instalada não tem Instagram nem
 * horário definido, e obrigar um valor levaria alguém a inventar um. Campo vazio
 * some da tela; campo inventado vira informação errada com cara de verdadeira.
 */
@Entity
@Table(name = "shop_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class ShopSettings {

    /** O identificador da única linha que esta tabela pode ter. */
    public static final Integer ID_UNICO = 1;

    @Id
    @EqualsAndHashCode.Include
    private Integer id;

    /**
     * Só dígitos, com país e DDD, como o wa.me exige.
     *
     * <p>Guardar "(73) 99814-9668" faria o link virar
     * {@code wa.me/(73) 99814-9668}, que abre uma página de erro. A limpeza
     * acontece antes de chegar aqui, e o CHECK do banco é a última barreira.
     */
    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "delivery_city", length = 120)
    private String deliveryCity;

    /** O @ sem o arroba e sem URL: só o identificador do perfil. */
    @Column(name = "instagram_handle", length = 60)
    private String instagramHandle;

    @Column(name = "opening_hours", length = 180)
    private String openingHours;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    void aoAtualizar() {
        this.updatedAt = Instant.now();
    }
}
