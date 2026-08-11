package com.florescer.product.support;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.atomic.AtomicReference;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.sun.net.httpserver.HttpServer;

/**
 * Um auth-service de mentira, servindo apenas o JWKS.
 *
 * <p>Existe para que a rotação de chave possa ser testada de verdade: o que
 * precisa ser provado é que o product-service passa a aceitar uma chave nova sem
 * reiniciar, e isso só aparece se houver como trocar o conteúdo publicado com o
 * serviço no ar.
 *
 * <p>Usa o servidor HTTP do próprio JDK em vez de acrescentar uma dependência de
 * teste para isso.
 */
public final class FakeJwksServer implements AutoCloseable {

    private final HttpServer server;
    private final AtomicReference<KeyPair> parAtual = new AtomicReference<>();
    private final AtomicReference<String> corpoJwks = new AtomicReference<>();

    private FakeJwksServer(HttpServer server) {
        this.server = server;
    }

    public static FakeJwksServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            FakeJwksServer fake = new FakeJwksServer(server);
            fake.rotacionar();

            server.createContext("/.well-known/jwks.json", exchange -> {
                byte[] corpo = fake.corpoJwks.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, corpo.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(corpo);
                }
            });

            server.start();
            return fake;
        } catch (IOException ex) {
            throw new IllegalStateException("Nao foi possivel subir o servidor JWKS de teste", ex);
        }
    }

    public String jwksUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/.well-known/jwks.json";
    }

    /** O par que está publicado agora, para assinar tokens que devem ser aceitos. */
    public KeyPair parAtual() {
        return parAtual.get();
    }

    /**
     * Publica um par novo no lugar do anterior, como faria uma troca de chave.
     *
     * <p>O {@code kid} muda junto, porque é derivado da chave: é o que faz quem
     * valida perceber que precisa buscar o conjunto de novo.
     */
    public void rotacionar() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair novo = generator.generateKeyPair();

            RSAKey jwk = new RSAKey.Builder((RSAPublicKey) novo.getPublic())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyIDFromThumbprint()
                    .build();

            parAtual.set(novo);
            corpoJwks.set(new JWKSet(jwk).toString());
        } catch (JOSEException | java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Nao foi possivel rotacionar a chave de teste", ex);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
