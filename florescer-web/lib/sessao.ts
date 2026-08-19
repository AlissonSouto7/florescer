'use client';

/**
 * Guarda o token de acesso no navegador.
 *
 * Fica em `sessionStorage`, e não em `localStorage`: o token vale uma hora e
 * some quando a aba fecha, o que reduz a janela em que uma sessão esquecida num
 * computador compartilhado continua válida.
 *
 * Nenhum dos dois protege contra XSS, e é por isso que o resto da aplicação
 * evita `innerHTML` e o servidor manda `X-Content-Type-Options`. A proteção de
 * verdade seria cookie `HttpOnly`, que exige o backend emitir o cookie e está
 * registrado como dívida.
 */

const CHAVE = 'florescer.token';

export function guardarToken(token: string): void {
  sessionStorage.setItem(CHAVE, token);
}

export function lerToken(): string | null {
  if (typeof window === 'undefined') return null;
  return sessionStorage.getItem(CHAVE);
}

export function esquecerToken(): void {
  sessionStorage.removeItem(CHAVE);
}

/** Papéis do token, para a interface esconder o que a pessoa não pode fazer. */
export function papeis(token: string): string[] {
  try {
    const payload = token.split('.')[1];
    // O payload é base64url: troca os caracteres antes de decodificar, senão
    // token com "-" ou "_" quebra.
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const scope: string = JSON.parse(json).scope ?? '';
    return scope.split(' ').filter(Boolean);
  } catch {
    return [];
  }
}

/**
 * Se o token expirou, medido pelo próprio token.
 *
 * Evita a tela pedir dados e só descobrir no 401 que a sessão acabou. Não é
 * verificação de segurança: quem decide é o servidor, que confere a assinatura.
 * Aqui é só para não mostrar um formulário que vai falhar.
 */
export function expirado(token: string): boolean {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const exp: number | undefined = JSON.parse(json).exp;
    return exp === undefined || exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

export function ehAdmin(token: string): boolean {
  return papeis(token).includes('ADMIN');
}
