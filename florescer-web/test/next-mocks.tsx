/**
 * Substitutos para os componentes do Next que precisam do runtime dele.
 *
 * `next/image` otimiza a imagem no servidor e `next/link` faz prefetch pelo
 * roteador. Nenhum dos dois funciona fora de uma aplicação Next em execução, e
 * nenhum dos dois é o que os testes verificam: o que importa é a URL que chega
 * ao `src` e o destino do link.
 *
 * Uso, no topo do arquivo de teste:
 *
 *     vi.mock('next/image', () => imagemFalsa());
 *     vi.mock('next/link', () => linkFalso());
 */

export function imagemFalsa() {
  return {
    default: ({ src, alt }: { src: string; alt: string }) => <img src={src} alt={alt} />,
  };
}

export function linkFalso() {
  return {
    default: ({ href, children }: { href: string; children: React.ReactNode }) => <a href={href}>{children}</a>,
  };
}
