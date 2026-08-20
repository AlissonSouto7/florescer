-- Os dados da loja que a vendedora precisa trocar sozinha.
--
-- O número do WhatsApp vivia numa variável de ambiente do servidor, e o rodapé
-- tinha texto fixo no código. Nos dois casos, mudar exigia alguém editar arquivo
-- e reiniciar container: a vendedora não consegue, e nem deveria precisar. Um
-- número desatualizado é pior que qualquer bug daqui, porque o pedido
-- simplesmente não chega em ninguém e nada na tela indica isso.
--
-- Uma linha só, e é isso mesmo.
--
-- A tabela guarda a configuração de UMA loja, não de várias. O `id` fixo em 1,
-- com CHECK, é o que garante isso no banco: sem ele, um bug no código criaria
-- uma segunda linha e passaria a existir a pergunta "qual das duas vale?", que
-- nenhuma leitura conseguiria responder. Uma tabela chave-valor daria o mesmo
-- resultado com mais indireção e sem tipo em nada.
CREATE TABLE shop_settings (
    id                  INTEGER      PRIMARY KEY,
    whatsapp_number     VARCHAR(20),
    delivery_city       VARCHAR(120),
    instagram_handle    VARCHAR(60),
    opening_hours       VARCHAR(180),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_shop_settings_singleton CHECK (id = 1)
);

-- Todas as colunas aceitam NULL de propósito: uma loja recém-instalada não tem
-- Instagram nem horário definido, e obrigar um valor faria alguém inventar um.
-- Campo vazio some da tela; campo inventado vira informação errada com cara de
-- verdadeira.

-- O número aceita só dígitos, entre 10 e 15.
--
-- 15 é o teto do padrão E.164, e 10 cobre um fixo com DDD. A validação também
-- existe na aplicação, com mensagem por campo; aqui ela é a última barreira,
-- para o dado continuar válido mesmo que alguém escreva no banco direto.
ALTER TABLE shop_settings
    ADD CONSTRAINT ck_shop_settings_whatsapp
    CHECK (whatsapp_number IS NULL OR whatsapp_number ~ '^[0-9]{10,15}$');

-- O @ do Instagram, sem o arroba e sem URL: as regras do próprio Instagram são
-- letras, números, ponto e sublinhado, até 30 caracteres. Guardar
-- "instagram.com/loja/" aqui faria o rodapé montar um link quebrado.
ALTER TABLE shop_settings
    ADD CONSTRAINT ck_shop_settings_instagram
    CHECK (instagram_handle IS NULL OR instagram_handle ~ '^[A-Za-z0-9._]{1,30}$');

-- A linha nasce junto com a tabela, vazia.
--
-- Sem ela, toda leitura precisaria tratar "ainda não existe configuração", e o
-- primeiro salvamento seria um INSERT enquanto os seguintes seriam UPDATE. Com
-- a linha criada aqui, ler é sempre SELECT e salvar é sempre UPDATE.
INSERT INTO shop_settings (id) VALUES (1);
