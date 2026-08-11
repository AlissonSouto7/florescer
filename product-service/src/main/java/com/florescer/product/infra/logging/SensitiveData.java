package com.florescer.product.infra.logging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Converte dado pessoal em um identificador estável para uso em log.
 *
 * <p>Registrar tentativa de login e de registro é necessário para auditoria e
 * para perceber ataque em andamento. Registrar o e-mail em texto puro para isso
 * não é: o log costuma ter retenção mais longa que o banco, acesso mais amplo e
 * cópia em agregador externo, então um dado protegido por permissão no banco
 * acaba disponível em vários lugares.
 *
 * <p>O pseudônimo mantém o que a auditoria precisa: tentativas do mesmo e-mail
 * produzem o mesmo valor, então dá para contar e correlacionar sem saber de quem
 * se trata.
 *
 * <p>Não é anonimização. Como o conjunto de e-mails possíveis é adivinhável, quem
 * tiver o log e uma lista de candidatos consegue testar hipóteses. É por isso que
 * existe o salt: sem conhecê-lo, essa verificação deixa de ser viável.
 */
public final class SensitiveData {

    private static final int PSEUDONYM_LENGTH = 12;

    private final String salt;

    public SensitiveData(String salt) {
        this.salt = salt == null ? "" : salt;
    }

    /**
     * Devolve um identificador curto e estável para o valor informado.
     * Entrada nula ou em branco vira {@code "-"}, para não gerar um pseudônimo
     * que pareça um usuário real.
     */
    public String pseudonymize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + value.trim().toLowerCase()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, PSEUDONYM_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
