package com.florescer.product.domain.exception.custom;

import java.util.Map;

import lombok.Getter;

/**
 * Um dado da loja que não serve, com a mensagem no campo que o causou.
 *
 * <p>Existe porque a validação do telefone só pode acontecer <b>depois</b> da
 * limpeza. A pessoa digita "+55 (73) 99814-9668", e é o número sem pontuação que
 * precisa ter entre 10 e 15 dígitos. Validar antes recusaria exatamente o
 * formato que ela costuma usar, e obrigá-la a digitar sem parênteses seria fazer
 * a pessoa trabalhar por não querermos.
 *
 * <p>Carrega um mapa de campo para mensagem, e não um texto solto, para a tela
 * conseguir mostrar o aviso ao lado do campo certo. É o mesmo formato que a
 * Bean Validation já produz, então o frontend trata os dois do mesmo jeito.
 */
@Getter
public class InvalidSettingsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Map<String, String> errosPorCampo;

    public InvalidSettingsException(String campo, String mensagem) {
        super(mensagem);
        this.errosPorCampo = Map.of(campo, mensagem);
    }
}
