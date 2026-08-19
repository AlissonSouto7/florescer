import type { NextConfig } from 'next';

/**
 * O servidor do Next é a única porta aberta para a internet.
 *
 * O navegador fala só com este domínio. As chamadas para `/api/**` e as fotos em
 * `/uploads/**` são repassadas daqui para os serviços, pela rede interna. As
 * APIs não precisam estar publicadas, e uma porta exposta em vez de três é a
 * diferença mais barata de segurança que este projeto tem.
 *
 * Dois efeitos que vêm junto:
 *
 * - **A mesma imagem serve todos os ambientes.** O único endereço que ela
 *   conhece é o interno, que é o nome do serviço na rede do Docker e não muda
 *   entre dev, staging e produção. Antes, o endereço público ia embutido no
 *   JavaScript durante o build (é o que `NEXT_PUBLIC_*` faz), o que obrigaria a
 *   reconstruir por ambiente e faria a imagem testada em staging deixar de ser
 *   a que vai para produção.
 * - **CORS deixa de existir**, porque tudo vem da mesma origem.
 *
 * Sobre as fotos, há uma armadilha específica do Next 16: ele recusa otimizar
 * imagem cujo host resolve para IP privado, como proteção contra SSRF, e a
 * única pista fica no log do servidor:
 *
 *   upstream image ... hostname resolved to private IP ["::1","127.0.0.1"]
 *
 * Tanto `product-service` (rede do Docker) quanto `localhost` são privados,
 * então toda foto falhava com 400. Existe uma opção `dangerouslyAllowLocalIP`
 * para desligar a checagem: o nome é honesto e a resposta é não, porque ela
 * reabriria o buraco justamente onde a URL vem de dado do banco. Passando pelo
 * rewrite, a imagem é same-origin e quem busca do backend é o servidor do Next,
 * para um host fixo definido aqui.
 *
 * Uma armadilha vale registrar: o destino do rewrite é resolvido no **build**,
 * não em runtime. Mudar `PRODUCT_API` só no ambiente do container não tem
 * efeito; ele precisa ir como `ARG` no Dockerfile também.
 */
const PRODUTOS = process.env.PRODUCT_API ?? 'http://localhost:8081';
const AUTENTICACAO = process.env.AUTH_API ?? 'http://localhost:8080';

const nextConfig: NextConfig = {
  images: {
    // Só imagens do próprio domínio, e só sob /uploads. O padrão local é mais
    // restritivo que o remoto por natureza: não há host de terceiro envolvido.
    localPatterns: [{ pathname: '/uploads/**', search: '' }],
  },

  async rewrites() {
    return [
      // O catálogo e o login, vistos do navegador. O prefixo /api não colide com
      // rota nenhuma: estes rewrites são avaliados depois do sistema de
      // arquivos, então um route handler futuro em app/api venceria.
      { source: '/api/product', destination: `${PRODUTOS}/v1/product` },
      { source: '/api/product/:caminho*', destination: `${PRODUTOS}/v1/product/:caminho*` },
      { source: '/api/auth/:caminho*', destination: `${AUTENTICACAO}/v1/auth/:caminho*` },

      // As fotos das plantas.
      { source: '/uploads/:caminho*', destination: `${PRODUTOS}/uploads/:caminho*` },
    ];
  },

  // O container roda o build standalone: leva só o necessário para executar,
  // em vez de node_modules inteiro.
  output: 'standalone',

  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          { key: 'X-Content-Type-Options', value: 'nosniff' },
          { key: 'X-Frame-Options', value: 'DENY' },
          { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
        ],
      },
    ];
  },
};

export default nextConfig;
