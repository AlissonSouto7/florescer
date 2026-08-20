package com.florescer.product.domain.service;

import com.florescer.product.domain.entity.ShopSettings;
import com.florescer.product.domain.model.ShopSettingsChanges;

/** Leitura e alteração dos dados da loja. */
public interface ShopSettingsService {

    /**
     * Os dados atuais.
     *
     * <p>Sempre devolve uma configuração, nunca vazio: a linha nasce com a
     * migration, então quem lê não precisa tratar "ainda não configurado".
     */
    ShopSettings obter();

    /** Grava o que a vendedora mandou, já limpo e normalizado. */
    ShopSettings salvar(ShopSettingsChanges mudancas);
}
