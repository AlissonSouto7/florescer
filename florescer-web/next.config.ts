import type { NextConfig } from 'next';

/**
 * As imagens das plantas vêm do product-service, não deste projeto. O
 * componente Image só carrega de domínios declarados aqui: sem isso, qualquer
 * URL na resposta da API viraria uma requisição feita pelo nosso servidor, o
 * que é um caminho pronto para SSRF.
 *
 * O host vem de variável porque muda por ambiente. Em produção, o valor precisa
 * apontar para o domínio real que serve os uploads.
 */
const hostDasImagens = new URL(process.env.NEXT_PUBLIC_PRODUCT_API ?? 'http://localhost:8081');

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: hostDasImagens.protocol.replace(':', '') as 'http' | 'https',
        hostname: hostDasImagens.hostname,
        port: hostDasImagens.port,
        pathname: '/uploads/**',
      },
    ],
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
