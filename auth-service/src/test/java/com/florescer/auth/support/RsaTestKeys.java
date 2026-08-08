package com.florescer.auth.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Gera um par RSA novo a cada execução da suíte.
 *
 * <p>Os testes nunca usam o par real da aplicação: a chave de produção não pode estar
 * no repositório nem no ambiente de CI.
 */
public final class RsaTestKeys {

    private static final KeyPair KEY_PAIR = generate();

    private RsaTestKeys() {
    }

    public static String privateKeyPem() {
        return pem("PRIVATE KEY", KEY_PAIR.getPrivate().getEncoded());
    }

    public static String publicKeyPem() {
        return pem("PUBLIC KEY", KEY_PAIR.getPublic().getEncoded());
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel gerar o par RSA de teste", ex);
        }
    }

    private static String pem(String type, byte[] encoded) {
        String body = Base64.getMimeEncoder(64, System.lineSeparator().getBytes()).encodeToString(encoded);
        return "-----BEGIN " + type + "-----" + System.lineSeparator()
                + body + System.lineSeparator()
                + "-----END " + type + "-----" + System.lineSeparator();
    }
}
