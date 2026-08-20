/**
 * O endereço público da loja.
 *
 * Diferente de tudo o mais no frontend, isto precisa ser um endereço absoluto:
 * `sitemap.xml`, as tags de compartilhamento e os dados estruturados vão ser
 * lidos por um buscador ou por outro site, e caminho relativo não diz nada fora
 * daqui.
 *
 * Vem do ambiente e é lido em runtime, então a mesma imagem continua servindo
 * qualquer ambiente. Um valor errado aqui não quebra a tela: ele faz o Google
 * indexar endereços que não existem, o que é pior, porque ninguém percebe.
 */
const PADRAO = 'http://localhost:3000';

export function enderecoDoSite(): string {
  const valor = process.env.SITE_URL?.trim();
  if (!valor) return PADRAO;

  // Barra no fim duplicaria a barra em toda URL montada a partir daqui.
  return valor.replace(/\/+$/, '');
}
