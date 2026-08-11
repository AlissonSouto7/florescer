package com.florescer.product.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Captura o que a aplicação registrou em log, e apenas isso.
 *
 * <p>Capturar a saída do processo parece equivalente e não é: o ferramental de
 * teste também escreve ali. O MockMvc imprime a requisição completa no relatório
 * quando uma asserção falha, corpo e senha incluídos, então um teste que lê o
 * stdout acaba acusando o relatório de falha de outro teste. Aqui os eventos vêm
 * do appender, então o que se afirma é o que a aplicação gravaria em produção.
 *
 * <p>O appender é do Logback, e não do Log4j2, embora o código use
 * {@code @Log4j2}: essa anotação gera apenas a API, e no Spring Boot as chamadas
 * chegam ao Logback por uma ponte. O backend real é quem grava.
 */
public final class LogCapture implements AutoCloseable {

    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final Logger rootLogger;
    private final AppenderBase<ILoggingEvent> appender;
    private final Level originalLevel;

    private LogCapture() {
        this.rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        this.originalLevel = rootLogger.getLevel();

        this.appender = new AppenderBase<>() {
            @Override
            protected void append(ILoggingEvent event) {
                messages.add(event.getFormattedMessage());
                // O MDC também vai para a linha final, pelo pattern em
                // desenvolvimento e pelo includeMdc em produção. Ignorá-lo aqui
                // deixaria de fora tanto o correlationId, que é o que se quer
                // encontrar, quanto qualquer dado pessoal que alguém venha a
                // colocar nele por engano.
                if (event.getMDCPropertyMap() != null) {
                    event.getMDCPropertyMap().forEach((k, v) -> messages.add(k + "=" + v));
                }
                if (event.getThrowableProxy() != null) {
                    messages.add(String.valueOf(event.getThrowableProxy().getMessage()));
                }
            }
        };

        this.appender.start();
        this.rootLogger.addAppender(appender);
        this.rootLogger.setLevel(Level.DEBUG);
    }

    public static LogCapture start() {
        return new LogCapture();
    }

    /** Tudo o que a aplicação registrou desde o início da captura. */
    public String all() {
        return String.join("\n", messages);
    }

    @Override
    public void close() {
        rootLogger.detachAppender(appender);
        rootLogger.setLevel(originalLevel);
        appender.stop();
    }
}
