import type { NextConfig } from 'next';

/**
 * As fotos das plantas são servidas pelo product-service, mas chegam ao
 * navegador pelo domínio deste projeto, através do rewrite abaixo.
 *
 * O caminho direto não funciona, e o motivo é uma proteção do Next 16: ele
 * recusa otimizar imagem cujo host resolve para IP privado, porque um endereço
 * controlado pela resposta de uma API é um vetor de SSRF conhecido. Tanto
 * `product-service` (rede do Docker) quanto `localhost` são privados, então
 * toda foto falhava com 400 e a única pista estava no log do servidor:
 *
 *   upstream image ... hostname resolved to private IP ["::1","127.0.0.1"]
 *
 * Existe uma opção `dangerouslyAllowLocalIP` para desligar isso. O nome é
 * honesto e a resposta é não: ela reabriria exatamente o buraco que a checagem
 * fecha, num ponto onde a URL vem de dado do banco.
 *
 * Com o rewrite, a imagem passa a ser same-origin (`/uploads/x.png`), o Next a
 * trata como local, e quem busca do product-service é o servidor do Next, para
 * um host fixo definido aqui, e não para qualquer endereço que a API devolver.
 */
const PRODUTOS = process.env.PRODUCT_API ?? 'http://localhost:8081';

const nextConfig: NextConfig = {
  images: {
    // Só imagens do próprio domínio, e só sob /uploads. O padrão local é mais
    // restritivo que o remoto por natureza: não há host de terceiro envolvido.
    localPatterns: [{ pathname: '/uploads/**', search: '' }],
  },

  async rewrites() {
    return [
      {
        source: '/uploads/:caminho*',
        destination: `${PRODUTOS}/uploads/:caminho*`,
      },
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
