package com.florescer.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.florescer.auth.infrastructure.logging.SensitiveData;

@Configuration
public class LoggingConfig {

    /**
     * O salt precisa ser o mesmo entre reinícios para o pseudônimo continuar
     * comparável ao longo do tempo, e precisa ser secreto para que ninguém
     * consiga testar candidatos contra o log.
     *
     * <p>Sem valor configurado o pseudônimo continua funcionando: perde apenas a
     * resistência a teste de hipótese, o que ainda é melhor que e-mail em texto
     * puro. Por isso aqui há default, ao contrário da chave de assinatura.
     */
    @Bean
    SensitiveData sensitiveData(@Value("${app.logging.pseudonym-salt:}") String salt) {
        return new SensitiveData(salt);
    }
}
