import { describe, expect, it } from 'vitest';

import { AMBIENTE, DIFICULDADE, LUMINOSIDADE, REGA, altura, caminhoDaImagem, precoEmReal } from './rotulos';

/**
 * O que estes testes protegem
 *
 * `caminhoDaImagem` é a peça que neutraliza a issue #89: a API monta a URL da
 * foto a partir do host de quem chamou, então a mesma planta volta com
 * `product-service:8081` na renderização do servidor e com o endereço público
 * quando o painel busca do navegador. Se esta função voltar a devolver a URL
 * absoluta, nenhuma foto carrega, e a falha é silenciosa: a página monta, só as
 * imagens somem.
 *
 * Os rótulos protegem contra o enum vazar para a tela. `MEIA_SOMBRA` no lugar de
 * "Meia sombra" não quebra nada, e por isso passa despercebido até alguém ver.
 */

describe('caminhoDaImagem', () => {
  it('descarta o host de uma URL absoluta e guarda só o caminho', () => {
    expect(caminhoDaImagem('http://product-service:8081/uploads/foto.png')).toBe('/uploads/foto.png');
    expect(caminhoDaImagem('http://localhost:8081/uploads/foto.png')).toBe('/uploads/foto.png');
    expect(caminhoDaImagem('https://api.florescer.com.br/uploads/foto.png')).toBe('/uploads/foto.png');
  });

  it('devolve a mesma coisa para os dois hosts que a API alterna (issue #89)', () => {
    const doServidor = caminhoDaImagem('http://product-service:8081/uploads/a.png');
    const doNavegador = caminhoDaImagem('http://localhost:8081/uploads/a.png');
    expect(doServidor).toBe(doNavegador);
  });

  it('mantém o caminho quando já é relativo', () => {
    expect(caminhoDaImagem('/uploads/foto.png')).toBe('/uploads/foto.png');
  });

  it('acrescenta a barra inicial que o next/image exige', () => {
    expect(caminhoDaImagem('uploads/foto.png')).toBe('/uploads/foto.png');
  });

  it('cai no placeholder quando não há imagem', () => {
    expect(caminhoDaImagem(null)).toBe('/placeholder.svg');
    expect(caminhoDaImagem(undefined)).toBe('/placeholder.svg');
    expect(caminhoDaImagem('')).toBe('/placeholder.svg');
  });

  it('preserva o nome do arquivo com acento e espaço', () => {
    // O backend nomeia por UUID, mas a função não pode presumir isso: uma foto
    // antiga ou vinda de outra origem ainda pode ter nome com acento.
    expect(caminhoDaImagem('http://localhost:8081/uploads/violeta viçosa.png'))
      .toBe('/uploads/violeta%20vi%C3%A7osa.png');
  });
});

describe('precoEmReal', () => {
  it('escreve o preço como o Brasil escreve', () => {
    // \u00a0 é o espaço não separável que o Intl usa depois do R$.
    expect(precoEmReal(49.9)).toBe('R$\u00a049,90');
    expect(precoEmReal(1234.5)).toBe('R$\u00a01.234,50');
    expect(precoEmReal(0)).toBe('R$\u00a00,00');
  });
});

describe('altura', () => {
  it('mostra em centímetros abaixo de um metro', () => {
    expect(altura(40)).toBe('40 cm');
    expect(altura(99)).toBe('99 cm');
  });

  it('vira metro a partir de 100 cm, porque 120 cm é difícil de imaginar', () => {
    expect(altura(100)).toBe('1 m');
    expect(altura(120)).toBe('1,2 m');
    expect(altura(185)).toBe('1,9 m');
  });

  it('devolve nulo para planta cadastrada antes do campo existir', () => {
    expect(altura(null)).toBeNull();
  });
});

describe('rótulos', () => {
  it('traduz cada valor da API para o que a pessoa lê', () => {
    expect(LUMINOSIDADE.MEIA_SOMBRA).toBe('Meia sombra');
    expect(REGA.DUAS_A_TRES_VEZES_SEMANA).toBe('2 a 3 vezes por semana');
    expect(AMBIENTE.AMBOS).toBe('Dentro ou fora');
    expect(DIFICULDADE.FACIL).toBe('Fácil de cuidar');
  });

  it('não deixa nenhum enum sem tradução', () => {
    // Um valor novo no backend sem rótulo aqui apareceria cru na tela.
    for (const mapa of [LUMINOSIDADE, REGA, AMBIENTE, DIFICULDADE]) {
      for (const [chave, rotulo] of Object.entries(mapa)) {
        expect(rotulo, `${chave} sem rótulo`).toBeTruthy();
        expect(rotulo, `${chave} não foi traduzido`).not.toMatch(/^[A-Z_]+$/);
      }
    }
  });
});
