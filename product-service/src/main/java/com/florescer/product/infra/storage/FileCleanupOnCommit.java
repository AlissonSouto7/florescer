package com.florescer.product.infra.storage;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Adia efeitos no disco para depois que a transação do banco confirmar.
 *
 * <p>O sistema de arquivos não participa da transação: apagar um arquivo dentro
 * dela é definitivo mesmo que o banco desfaça tudo em seguida. Isso produzia
 * dois estados incoerentes.
 *
 * <p>No PATCH, a imagem antiga era removida <b>antes</b> de a nova ser gravada;
 * uma falha no meio deixava o produto sem imagem alguma. E no DELETE, o arquivo
 * sumia antes do banco confirmar, então um rollback deixava o produto vivo
 * apontando para um arquivo inexistente.
 *
 * <p>Registrando a remoção para depois do commit, o arquivo só desaparece quando
 * a mudança no banco é definitiva. O caminho oposto, gravar um arquivo e o banco
 * falhar, deixa um arquivo órfão: isso é aceitável, porque órfão ocupa espaço e
 * não quebra ninguém, ao contrário de uma referência apontando para o vazio.
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class FileCleanupOnCommit {

    private final ImageStorageService imageStorageService;

    /**
     * Agenda a remoção do arquivo para depois do commit.
     *
     * <p>Sem transação ativa, remove na hora: é o que acontece se o método for
     * chamado fora de um contexto transacional, e adiar não faria sentido.
     */
    public void deleteAfterCommit(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            imageStorageService.deleteOldImageFromDisk(imagePath);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imageStorageService.deleteOldImageFromDisk(imagePath);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    // O banco desfez, então o arquivo continua sendo o correto e
                    // permanece onde está.
                    log.debug("Transação não confirmada: imagem preservada.");
                }
            }
        });
    }
}
