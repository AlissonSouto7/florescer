import '@testing-library/jest-dom/vitest';

/**
 * `matchMedia` no jsdom.
 *
 * O jsdom não implementa nada de media query, e chamar `window.matchMedia`
 * simplesmente estoura. Sem esta ponte, todo componente que decide algo por
 * tamanho de tela só pode ser verificado no navegador, e foi assim que passou
 * um defeito real: a trava de rolagem da gaveta de filtros era aplicada também
 * no computador, onde gaveta nenhuma existe, e a barra de rolagem sumia da
 * página inteira.
 *
 * A implementação lê `window.innerWidth`, que o jsdom tem e o teste pode
 * mudar. Assim o teste declara em qual tela está, em vez de ligar e desligar um
 * mock booleano sem relação com a realidade.
 *
 * O padrão do jsdom é 1024px de largura, ou seja, computador.
 */
const ouvintes = new Set<() => void>();

window.matchMedia = (consulta: string): MediaQueryList => {
  const combina = () => {
    const teto = /\(\s*max-width:\s*([\d.]+)px\s*\)/.exec(consulta);
    if (teto) return window.innerWidth <= parseFloat(teto[1]);

    const piso = /\(\s*min-width:\s*([\d.]+)px\s*\)/.exec(consulta);
    if (piso) return window.innerWidth >= parseFloat(piso[1]);

    // Consulta que esta ponte não entende nunca combina, em vez de combinar por
    // acidente: um falso positivo aqui faria o teste passar pelo caminho errado.
    return false;
  };

  const lista = {
    media: consulta,
    onchange: null,
    get matches() {
      return combina();
    },
    addEventListener: (_: string, ouvinte: () => void) => {
      ouvintes.add(ouvinte);
    },
    removeEventListener: (_: string, ouvinte: () => void) => {
      ouvintes.delete(ouvinte);
    },
    addListener: (ouvinte: () => void) => {
      ouvintes.add(ouvinte);
    },
    removeListener: (ouvinte: () => void) => {
      ouvintes.delete(ouvinte);
    },
    dispatchEvent: () => false,
  };

  return lista as unknown as MediaQueryList;
};

/**
 * Muda a largura da tela e avisa quem estava ouvindo, como o navegador faz.
 *
 * Existe para o teste poder exercitar o que acontece **durante** o
 * redimensionamento, e não só o estado inicial: girar o celular ou arrastar a
 * janela com um painel aberto é justamente onde esse tipo de trava fica presa.
 */
export function larguraDaTela(pixels: number) {
  Object.defineProperty(window, 'innerWidth', {
    value: pixels,
    writable: true,
    configurable: true,
  });
  ouvintes.forEach((ouvinte) => ouvinte());
}
