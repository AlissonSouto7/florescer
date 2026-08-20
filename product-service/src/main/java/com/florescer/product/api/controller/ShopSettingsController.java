package com.florescer.product.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.florescer.product.api.dto.request.ShopSettingsRequest;
import com.florescer.product.api.dto.response.ShopSettingsResponse;
import com.florescer.product.config.SecurityConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

/**
 * Os dados da loja: WhatsApp, cidade, Instagram e horário.
 *
 * <p>A leitura é pública e a escrita é de ADMIN, e essa assimetria é
 * proposital: o rodapé e o botão de comprar da vitrine precisam desses dados
 * sem login, mas alterá-los é ação de quem vende.
 */
@RequestMapping("/v1/settings")
public interface ShopSettingsController {

    @GetMapping
    @Operation(summary = "Dados da loja",
            description = "Público: a vitrine usa no rodapé e no botão de comprar.")
    @ApiResponse(responseCode = "200", description = "Dados atuais, com nulo no que não foi preenchido")
    ResponseEntity<ShopSettingsResponse> obter();

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = SecurityConfig.SECURITY)
    @Operation(summary = "Altera os dados da loja",
            description = "Só ADMIN. O número é gravado apenas com dígitos e o Instagram apenas com o perfil, "
                    + "independentemente de como tenham sido digitados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gravado, já normalizado"),
            @ApiResponse(responseCode = "400", description = "Número fora do formato aceito"),
            @ApiResponse(responseCode = "401", description = "Sem token"),
            @ApiResponse(responseCode = "403", description = "Token sem ADMIN")
    })
    ResponseEntity<ShopSettingsResponse> salvar(@RequestBody @Valid ShopSettingsRequest request);
}
