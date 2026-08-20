# florescer-web

A loja: vitrine para quem compra, painel para quem vende. Porta 3000, Next.js 16 com React 19.

É também o **proxy** da stack. O navegador fala só com este domínio, e as chamadas para `/api/**` e `/uploads/**` são repassadas daqui para os serviços pela rede interna. É o que permite manter as APIs fora da internet.

Para visão geral, arquitetura e como subir tudo, ver o [README da raiz](../README.md).

## Rotas

| Rota | Quem acessa | O que faz |
|---|---|---|
| `/` | qualquer um | vitrine com filtros |
| `/planta/[id]` | qualquer um | detalhe e botão de WhatsApp |
| `/login` | qualquer um | entrada da vendedora |
| `/admin` | ADMIN | lista das plantas, com editar e excluir |
| `/admin/nova`, `/admin/[id]` | ADMIN | cadastro e edição |
| `/admin/configuracoes` | ADMIN | WhatsApp, cidade de entrega, Instagram e horário |

**Não existe link para `/admin` em lugar nenhum do site**, e isso é escolha. Quem cadastra é uma pessoa só, que salva o endereço nos favoritos; para todo o resto do mundo aquele caminho não existe. Isso é discrição, não segurança: quem protege é o backend, que recusa qualquer requisição sem token de ADMIN.

## Decisões que não são óbvias

**Nenhuma variável começa com `NEXT_PUBLIC_`.** Esse prefixo grava o valor dentro do JavaScript que vai para o navegador, **durante o build**, e traria dois problemas: a imagem passaria a conter o endereço de um ambiente específico, e o navegador passaria a falar direto com as APIs, o que obrigaria a publicá-las.

**O destino dos rewrites é resolvido no build**, não em runtime. `PRODUCT_API` e `AUTH_API` precisam ir como `ARG` no Dockerfile; passá-las só no `environment` do container não tem efeito nenhum, e a falha é silenciosa.

**A vitrine é renderizada no servidor.** O HTML chega com as plantas dentro, então o buscador indexa cada uma e quem abre no celular vê o conteúdo antes de o JavaScript baixar.

**O filtro vive na URL.** Sobrevive ao recarregar, pode ser enviado por link, e o botão voltar desfaz um filtro em vez de sair da loja.

**Os dados da loja não têm cache.** A vendedora salvava o número, abria a loja e via o valor antigo. O custo de não guardar foi medido antes de decidir: 162 bytes e cerca de 17 ms por página, numa chamada que não sai da máquina.

**As páginas de `app/` não têm teste**, e entram no relatório de cobertura com zero de propósito. São componentes de servidor que buscam da API e montam a tela; testá-las em jsdom exigiria simular o runtime do Next inteiro, e o que sobraria de garantia é o que os testes de componente já cobrem. Esconder esse zero faria o número global parecer melhor do que é.

## Configuração

| Variável | Obrigatória | O que é |
|---|---|---|
| `PRODUCT_API` | sim | onde o servidor do Next encontra o catálogo. **Também é argumento de build** |
| `AUTH_API` | sim | idem, para a identidade |
| `SITE_URL` | não | endereço público, usado no sitemap, no robots e na prévia do link compartilhado |

O WhatsApp, a cidade de entrega, o Instagram e o horário **não são configuração**: ficam no banco e a vendedora os edita em `/admin/configuracoes`.

## Desenvolvimento

```bash
npm install
npm run dev              # http://localhost:3000
```

Rodando fora do Docker, copie `.env.example` para `.env.local` e aponte `PRODUCT_API` e `AUTH_API` para as portas locais dos serviços.

## Testes

```bash
npm test                 # a suíte
npm run test:coverage    # com o gate de cobertura
```

Vitest com Testing Library, em jsdom. O gate tem **piso por pasta**, e não um piso global: `lib/**` exige 95% de linhas e 85% de ramos, `components/**` exige 93% e 88%. Um piso global misturaria essas pastas com as páginas de `app/` e daria um número que não diz nada.

O que cada arquivo de teste protege, as rodadas de mutação e os achados de segurança estão em [`docs/features/vitrine.md`](../docs/features/vitrine.md).
