import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Planta } from '@/lib/api';
import { PLANTA } from '@/test/fixtures';

/**
 * O que estes testes protegem
 *
 * Este botão é o único caminho entre quem quer comprar e quem vende. Tudo o que
 * ele faz de errado custa uma venda, e nada disso aparece na tela:
 *
 * - mensagem sem codificar: acento vira lixo e o "&" de um nome corta o texto ao
 *   meio, então a vendedora recebe "Tenho interesse na Espada de S" e não sabe
 *   qual planta;
 * - botão em planta sem estoque: chega pedido que ela não pode atender, e
 *   alguém fica esperando resposta de uma venda que não existe;
 * - número não configurado: `wa.me/` sem número abre uma página de erro do
 *   WhatsApp, o que é pior que não ter botão.
 *
 * O número é lido uma vez, quando o módulo carrega. Por isso cada caso reimporta
 * o componente depois de trocar a variável de ambiente.
 */

const NUMERO = '5511999999999';

/** Carrega o componente com o número de ambiente que o caso precisa. */
// `null` e não `undefined` para o caso "sem número": undefined aciona o valor
// padrão do parâmetro, e o teste passaria a exercitar o caso oposto.
async function montar(planta: Planta, numero: string | null = NUMERO) {
  vi.resetModules();
  vi.stubEnv('NEXT_PUBLIC_WHATSAPP', numero ?? '');

  const { BotaoWhatsApp } = await import('./BotaoWhatsApp');
  return render(<BotaoWhatsApp planta={planta} />);
}

/** O texto que a vendedora vai receber, já decodificado. */
function mensagemDoLink(): string {
  const link = screen.getByRole('link') as HTMLAnchorElement;
  return new URL(link.href).searchParams.get('text') ?? '';
}

beforeEach(() => {
  vi.stubEnv('NEXT_PUBLIC_WHATSAPP', NUMERO);
});

afterEach(() => {
  vi.unstubAllEnvs();
});

describe('planta disponível', () => {
  it('aponta para o número configurado', async () => {
    await montar(PLANTA);
    const link = screen.getByRole('link') as HTMLAnchorElement;
    expect(new URL(link.href).hostname).toBe('wa.me');
    expect(new URL(link.href).pathname).toBe(`/${NUMERO}`);
  });

  it('já diz qual planta e por quanto', async () => {
    await montar(PLANTA);
    expect(mensagemDoLink()).toBe('Olá! Tenho interesse na Espada de São Jorge (R$\u00a049,90) que vi no site.');
  });

  it('não corrompe acento na mensagem', async () => {
    await montar({ ...PLANTA, name: 'Violeta viçosa' });
    expect(mensagemDoLink()).toContain('Violeta viçosa');
    // Na URL crua o acento tem que estar percent-encoded, não solto.
    const href = (screen.getByRole('link') as HTMLAnchorElement).getAttribute('href')!;
    expect(href).toContain('vi%C3%A7osa');
  });

  it('não deixa "&" no nome cortar a mensagem ao meio', async () => {
    // Sem encodeURIComponent, o "&" viraria separador de parâmetro e tudo depois
    // dele sumiria da mensagem.
    await montar({ ...PLANTA, name: 'Costela & Jiboia' });
    expect(mensagemDoLink()).toContain('Costela & Jiboia');
    expect(mensagemDoLink()).toContain('que vi no site.');
  });

  it('não deixa "#" no nome truncar a URL', async () => {
    await montar({ ...PLANTA, name: 'Cacto #7' });
    expect(mensagemDoLink()).toContain('Cacto #7');
  });

  it('abre em outra aba sem dar acesso a esta janela', async () => {
    await montar(PLANTA);
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('target', '_blank');
    // Sem noopener, a página aberta alcança window.opener e pode trocar esta
    // aba por uma cópia falsa da loja.
    expect(link.getAttribute('rel')).toContain('noopener');
  });
});

describe('planta que não pode ser vendida', () => {
  it('não oferece o botão quando o estoque zerou', async () => {
    await montar({ ...PLANTA, quantityStock: 0 });
    expect(screen.queryByRole('link')).toBeNull();
    expect(screen.getByText(/indisponível/i)).toBeInTheDocument();
  });

  it('não oferece o botão quando a vendedora tirou de venda', async () => {
    await montar({ ...PLANTA, availability: false });
    expect(screen.queryByRole('link')).toBeNull();
    expect(screen.getByText(/indisponível/i)).toBeInTheDocument();
  });

  it('exige as duas condições, não uma delas', async () => {
    // availability true com estoque 0 é o estado real de quem vendeu a última e
    // ainda não desmarcou.
    await montar({ ...PLANTA, availability: true, quantityStock: 0 });
    expect(screen.queryByRole('link')).toBeNull();
  });
});

describe('número não configurado', () => {
  it('esconde o botão em vez de mandar para uma página de erro do WhatsApp', async () => {
    const { container } = await montar(PLANTA, null);
    expect(container).toBeEmptyDOMElement();
  });
});
