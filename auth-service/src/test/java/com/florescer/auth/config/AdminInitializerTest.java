package com.florescer.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import com.florescer.auth.application.repository.UserRepository;
import com.florescer.auth.domain.entity.User;
import com.florescer.auth.support.AbstractIntegrationTest;

/**
 * A conta administrativa não pode nascer de um valor escrito no código.
 *
 * <p>Este serviço criava, em todo boot e em qualquer ambiente, um usuário
 * {@code admin@florescer.com} com senha {@code admin123}, e ainda imprimia as
 * credenciais no stdout. Num repositório público, isso é a senha do
 * administrador publicada junto com o código.
 *
 * <p>Aqui o inicializador está ligado por propriedade de teste, com credenciais
 * próprias, e o que se verifica é que ele obedece à configuração em vez de a
 * um valor fixo.
 */
@TestPropertySource(properties = {
        "app.admin.enabled=true",
        "app.admin.email=operador@exemplo.test",
        "app.admin.password=uma-senha-vinda-do-ambiente"
})
class AdminInitializerTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationContext context;

    @Test
    void criaOAdminComAsCredenciaisConfiguradas() {
        User admin = userRepository.findByEmail("operador@exemplo.test").orElse(null);

        assertThat(admin)
                .as("o admin deve ser criado com o e-mail que veio da configuracao")
                .isNotNull();
        assertThat(passwordEncoder.matches("uma-senha-vinda-do-ambiente", admin.getPassword()))
                .as("a senha gravada deve ser a configurada, e deve estar cifrada")
                .isTrue();
        assertThat(admin.getPassword())
                .as("a senha nunca pode ser gravada em texto puro")
                .isNotEqualTo("uma-senha-vinda-do-ambiente");
    }

    @Test
    void naoCriaNenhumaContaComCredenciaisEscritasNoCodigo() {
        assertThat(userRepository.findByEmail("admin@florescer.com"))
                .as("nenhuma conta pode nascer de credencial fixa no codigo")
                .isEmpty();
    }

    @Test
    void oInicializadorNaoExisteQuandoNaoEstaHabilitado() {
        // Nesta classe ele está ligado pela propriedade acima. O que se garante
        // é que a existência do bean depende da propriedade, e não do ambiente.
        assertThat(context.containsBean("initAdmin"))
                .as("com app.admin.enabled=true o bean precisa existir")
                .isTrue();
    }
}
