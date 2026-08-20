import type { Planta } from '@/lib/api';
import { caminhoDaImagem } from '@/lib/rotulos';
import { enderecoDoSite } from '@/lib/site';

/**
 * Descreve a planta em schema.org, para o buscador.
 *
 * O HTML diz onde o preço está na tela; isto diz que aquilo **é** um preço, em
 * reais, de um produto que está à venda. É o que faz o Google mostrar foto,
 * preço e disponibilidade no próprio resultado da busca, em vez de um link seco.
 * Para uma loja pequena, é a diferença entre aparecer e aparecer bem.
 *
 * Só descreve o que é verdade. Marcar como disponível uma planta esgotada
 * levaria alguém a clicar para encontrar "indisponível", e o Google penaliza
 * dado estruturado que não bate com a página.
 */
export function DadosEstruturados({ planta }: { planta: Planta }) {
  const site = enderecoDoSite();
  const disponivel = planta.availability && planta.quantityStock > 0;

  const dados = {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: planta.name,
    description: planta.description,
    image: `${site}${caminhoDaImagem(planta.imageUrl)}`,
    category: planta.type,
    offers: {
      '@type': 'Offer',
      url: `${site}/planta/${planta.id}`,
      priceCurrency: 'BRL',
      // Como string, e com ponto: o schema.org espera o formato numérico, não o
      // "R$ 49,90" que a pessoa lê na tela.
      price: planta.price.toFixed(2),
      availability: disponivel
        ? 'https://schema.org/InStock'
        : 'https://schema.org/OutOfStock',
      itemCondition: 'https://schema.org/NewCondition',
    },
  };

  return (
    <script
      type="application/ld+json"
      // O conteúdo é montado aqui, a partir de dado do próprio banco, e nunca de
      // entrada de terceiro. Ainda assim, `<` é escapado: um nome de planta com
      // "</script>" fecharia a tag e o resto viraria HTML executável.
      dangerouslySetInnerHTML={{ __html: JSON.stringify(dados).replace(/</g, '\\u003c') }}
    />
  );
}
