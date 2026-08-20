package com.florescer.product.api.controller.impl;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.florescer.product.api.controller.ShopSettingsController;
import com.florescer.product.api.dto.request.ShopSettingsRequest;
import com.florescer.product.api.dto.response.ShopSettingsResponse;
import com.florescer.product.domain.entity.ShopSettings;
import com.florescer.product.domain.model.ShopSettingsChanges;
import com.florescer.product.domain.service.ShopSettingsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ShopSettingsControllerImpl implements ShopSettingsController {

    private final ShopSettingsService service;

    @Override
    public ResponseEntity<ShopSettingsResponse> obter() {
        return ResponseEntity.ok(paraResposta(service.obter()));
    }

    @Override
    public ResponseEntity<ShopSettingsResponse> salvar(ShopSettingsRequest request) {
        ShopSettings salvo = service.salvar(new ShopSettingsChanges(
                request.whatsappNumber(),
                request.deliveryCity(),
                request.instagramHandle(),
                request.openingHours()));

        // Devolve o que ficou gravado, e não o que foi enviado: quem digitou
        // "@loja" precisa ver "loja" na tela depois de salvar, senão a próxima
        // edição parte de um valor que não é o do banco.
        return ResponseEntity.ok(paraResposta(salvo));
    }

    /**
     * Só o que a vitrine precisa mostrar.
     *
     * <p>A data da última alteração fica de fora de propósito: é informação
     * interna, e esta resposta é pública.
     */
    private ShopSettingsResponse paraResposta(ShopSettings settings) {
        return new ShopSettingsResponse(
                settings.getWhatsappNumber(),
                settings.getDeliveryCity(),
                settings.getInstagramHandle(),
                settings.getOpeningHours());
    }
}
