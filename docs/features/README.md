# Documentação por feature

Um documento por área do sistema, atualizado junto com o código.

| Documento | Cobre |
|---|---|
| [auth.md](auth.md) | registro, login, emissão de token, JWKS, rate limit |
| [products.md](products.md) | CRUD de produto, paginação, autorização por papel |
| [upload.md](upload.md) | recebimento, validação e entrega das imagens |
| [vitrine.md](vitrine.md) | a interface: vitrine, filtros, detalhe, login e painel da vendedora |
| [dados-da-loja.md](dados-da-loja.md) | WhatsApp, cidade de entrega, Instagram e horário, editáveis pela vendedora |
| [backup-e-deploy.md](backup-e-deploy.md) | cópia de segurança, restauração e verificação pós-deploy |

## Para que servem

Não são tutoriais. Existem para responder rápido a três perguntas que aparecem sempre:

**"Isso é seguro?"** Cada documento tem os achados de segurança com identificador próprio, separados em corrigidos, **abertos** (com o motivo de continuarem abertos) e **verificado e OK**. Esta última seção é a que economiza mais tempo: registra o que já foi investigado e não está quebrado, para ninguém gastar uma tarde reinvestigando.

**"Isso tem teste?"** Cada teste aparece ligado ao risco que protege, e não apenas listado pelo nome. Mais importante, cada documento diz explicitamente **o que não está coberto**. Um documento que só lista o caminho feliz não serve; o valor está em dizer onde estão os buracos.

**"Como eu confiro isso em produção?"** Comandos somente leitura, prontos para copiar. Nenhum deles altera estado.

## Convenção dos identificadores

`A-n` para autenticação, `P-n` para produtos, `U-n` para upload, `V-n` para a vitrine, `C-n` para os dados da loja. O número não é reaproveitado quando um achado é corrigido: ele muda de seção e mantém o identificador, para que uma referência antiga continue apontando para a mesma coisa.

## Ao mexer no código

Atualize o documento da área no mesmo pull request, não depois. Três seções envelhecem mais rápido que as outras e são as que enganam quem chega:

- **Achados abertos**: se um foi corrigido, ele muda de seção e ganha a data.
- **O que NÃO está coberto**: se um teste novo fechou um buraco, ele sai da lista.
- **Histórico**: uma linha com a data e o que mudou.

Documentação errada é pior que documentação ausente, porque é seguida.
