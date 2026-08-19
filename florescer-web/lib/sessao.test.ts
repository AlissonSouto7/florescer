import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ehAdmin, esquecerToken, expirado, guardarToken, lerToken } from './sessao';

/**
 * O que estes testes protegem
 *
 * O painel decide o que mostrar lendo o próprio token: se expirou, manda para o
 * login antes de montar um formulário que vai falhar; se não é ADMIN, não mostra
 * o painel. Nada disso é segurança (quem decide é o servidor, que confere a
 * assinatura), mas errar aqui deixa a vendedora presa numa tela que não salva.
 *
 * O caso do base64url merece teste próprio: um payload de JWT usa `-` e `_` no
 * lugar de `+` e `/`, e `atob` não aceita. Sem a troca, `atob` lança, o `catch`
 * devolve "expirado", e a pessoa é mandada para o login logo depois de logar.
 * O sintoma é um laço de login, e a causa está a três camadas de distância.
 */

/** Monta um JWT de mentira: só o payload importa, a assinatura nunca é lida aqui. */
function tokenCom(payload: Record<string, unknown>): string {
  const base64url = btoa(JSON.stringify(payload))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `cabecalho.${base64url}.assinatura`;
}

const AGORA = new Date('2026-08-19T12:00:00Z');
const emSegundos = (d: Date) => Math.floor(d.getTime() / 1000);

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(AGORA);
  sessionStorage.clear();
});

afterEach(() => {
  vi.useRealTimers();
});

describe('guardar e ler o token', () => {
  it('devolve o que foi guardado', () => {
    guardarToken('abc.def.ghi');
    expect(lerToken()).toBe('abc.def.ghi');
  });

  it('devolve nulo quando não há sessão', () => {
    expect(lerToken()).toBeNull();
  });

  it('esquece o token no logout', () => {
    guardarToken('abc.def.ghi');
    esquecerToken();
    expect(lerToken()).toBeNull();
  });

  it('não usa localStorage: a sessão morre com a aba', () => {
    guardarToken('abc.def.ghi');
    expect(localStorage.getItem('florescer.token')).toBeNull();
  });
});

describe('expirado', () => {
  it('reconhece token dentro da validade', () => {
    const daquiUmaHora = emSegundos(new Date(AGORA.getTime() + 3600_000));
    expect(expirado(tokenCom({ exp: daquiUmaHora }))).toBe(false);
  });

  it('reconhece token vencido', () => {
    const umMinutoAtras = emSegundos(new Date(AGORA.getTime() - 60_000));
    expect(expirado(tokenCom({ exp: umMinutoAtras }))).toBe(true);
  });

  it('trata como expirado o token que vence neste exato segundo', () => {
    // Preferir errar para o lado de mandar renovar: usar um token que vence
    // agora significa a requisição chegar no servidor já vencida.
    expect(expirado(tokenCom({ exp: emSegundos(AGORA) }))).toBe(true);
  });

  it('trata como expirado o token sem exp', () => {
    expect(expirado(tokenCom({ scope: 'ADMIN' }))).toBe(true);
  });

  it('trata como expirado qualquer coisa que não seja um JWT', () => {
    expect(expirado('isso-nao-e-token')).toBe(true);
    expect(expirado('')).toBe(true);
  });

  it('decodifica payload base64url, com - e _, sem cair no catch', () => {
    // Um payload cujo base64 tradicional contém "+" e "/". Se o código deixar de
    // trocar por "-" e "_", atob lança e a pessoa entra num laço de login.
    const payload = { exp: emSegundos(new Date(AGORA.getTime() + 3600_000)), sub: 'a?b>c~d' };
    const token = tokenCom(payload);
    expect(token.split('.')[1]).toMatch(/[-_]/); // o payload realmente exercita o caso
    expect(expirado(token)).toBe(false);
  });
});

describe('ehAdmin', () => {
  it('reconhece o escopo ADMIN', () => {
    expect(ehAdmin(tokenCom({ scope: 'ADMIN' }))).toBe(true);
  });

  it('reconhece ADMIN entre vários escopos', () => {
    expect(ehAdmin(tokenCom({ scope: 'USER ADMIN' }))).toBe(true);
  });

  it('recusa quem só tem USER', () => {
    expect(ehAdmin(tokenCom({ scope: 'USER' }))).toBe(false);
  });

  it('recusa token sem escopo nenhum', () => {
    expect(ehAdmin(tokenCom({ sub: 'alguem' }))).toBe(false);
  });

  it('recusa token corrompido em vez de lançar', () => {
    expect(ehAdmin('nada.disso.presta')).toBe(false);
  });

  it('decodifica payload base64url, com - e _, sem cair no catch', () => {
    // Mesmo caso do `expirado`, e precisa de teste próprio: a conversão está
    // escrita duas vezes, uma em cada função. Perder só a daqui faria a
    // vendedora logar e o painel não reconhecer que ela é admin.
    const token = tokenCom({ scope: 'ADMIN', sub: 'a?b>c~d' });
    expect(token.split('.')[1]).toMatch(/[-_]/); // o payload exercita o caso
    expect(ehAdmin(token)).toBe(true);
  });

  it('não confunde ADMINISTRADOR com ADMIN', () => {
    // O escopo é separado por espaço; comparar por "contém" daria ADMIN para
    // qualquer papel que comece com essas letras.
    expect(ehAdmin(tokenCom({ scope: 'ADMINISTRADOR' }))).toBe(false);
  });
});
