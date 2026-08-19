import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

/**
 * Os testes rodam em jsdom, não em navegador de verdade.
 *
 * A escolha é deliberada: o que precisa de cobertura aqui é lógica de
 * apresentação (a mensagem que vai para o WhatsApp, o preço com vírgula, a
 * conversão da URL da imagem), e isso não depende de motor de renderização real.
 * Um navegador de verdade custaria minutos por execução para verificar o mesmo.
 *
 * O que jsdom NÃO cobre continua sendo verificado no navegador: layout,
 * contraste, imagem que carrega de fato. Está registrado em
 * docs/features/vitrine.md.
 */
export default defineConfig({
  plugins: [react()],

  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./vitest.setup.ts'],
    include: ['**/*.test.{ts,tsx}'],

    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],

      // `app/` entra no relatório mesmo sem teste: esconder o zero faria o
      // número global parecer melhor do que é.
      include: ['lib/**/*.ts', 'components/**/*.tsx', 'app/**/*.tsx'],

      /**
       * Pisos por pasta, e não um piso global.
       *
       * As páginas de `app/` são componentes de servidor que buscam da API e
       * montam a tela. Testá-las em jsdom exigiria simular o runtime do Next
       * inteiro, e o que sobraria de garantia é o que os testes de componente
       * já cobrem. Um piso global misturaria as duas coisas e daria um número
       * que não diz nada.
       *
       * Os valores abaixo ficam logo abaixo do medido em 19/08/2026 (lib
       * 96,6% linhas / 88,1% ramos; components 95,5% / 91,7%): apertado o
       * bastante para acusar regressão, folgado o bastante para não quebrar a
       * cada refatoração.
       */
      thresholds: {
        'lib/**': { statements: 92, branches: 85, functions: 95, lines: 95 },
        'components/**': { statements: 92, branches: 88, functions: 90, lines: 93 },
      },
    },
  },

  resolve: {
    alias: { '@': import.meta.dirname },
  },
});
