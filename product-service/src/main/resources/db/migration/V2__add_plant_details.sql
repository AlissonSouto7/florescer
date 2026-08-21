-- Os campos que quem compra pergunta antes de fechar negócio.
--
-- Até aqui a planta tinha nome, descrição, preço e um texto livre de cuidados.
-- Esse texto serve para ler, não para garantir que a informação foi preenchida
-- nem para filtrar a vitrine, então perguntas como "que tamanho ela tem?" e
-- "posso deixar no sol?" continuavam indo para o WhatsApp antes de cada venda.
--
-- As colunas aceitam NULL de propósito, enquanto a API exige todas nos cadastros
-- novos. Uma coluna NOT NULL numa tabela que já tem linhas obriga a inventar um
-- valor para as existentes, e altura ou luminosidade inventadas apareceriam na
-- vitrine com a mesma cara de informação verdadeira. Planta antiga simplesmente
-- não mostra o campo; planta nova nasce completa.
--
-- Os enums são VARCHAR com CHECK, seguindo o que a V1 já faz com status: a
-- aplicação pode ser reescrita, os dados continuam válidos por conta própria.

ALTER TABLE product
    ADD COLUMN height_cm    INTEGER,
    ADD COLUMN light        VARCHAR(20),
    ADD COLUMN watering     VARCHAR(30),
    ADD COLUMN pet_safe     BOOLEAN,
    ADD COLUMN environment  VARCHAR(20),
    ADD COLUMN difficulty   VARCHAR(10),
    ADD COLUMN includes_pot BOOLEAN;

-- Altura em centímetros. O teto existe para barrar erro de digitação: 10 metros
-- de planta em vaso é engano de quem digitou, não uma venda.
ALTER TABLE product
    ADD CONSTRAINT ck_product_height CHECK (height_cm IS NULL OR (height_cm > 0 AND height_cm <= 1000));

ALTER TABLE product
    ADD CONSTRAINT ck_product_light CHECK (light IS NULL OR light IN ('SOL_PLENO', 'MEIA_SOMBRA', 'SOMBRA'));

ALTER TABLE product
    ADD CONSTRAINT ck_product_watering CHECK (watering IS NULL OR watering IN
        ('DIARIA', 'DUAS_A_TRES_VEZES_SEMANA', 'SEMANAL', 'QUINZENAL', 'MENSAL'));

ALTER TABLE product
    ADD CONSTRAINT ck_product_environment CHECK (environment IS NULL OR environment IN ('INTERNO', 'EXTERNO', 'AMBOS'));

ALTER TABLE product
    ADD CONSTRAINT ck_product_difficulty CHECK (difficulty IS NULL OR difficulty IN ('FACIL', 'MEDIO', 'DIFICIL'));

-- Índices para os filtros previstos na vitrine: "plantas para sombra" e
-- "seguras para o meu gato". Sem eles, cada filtro varre a tabela inteira.
CREATE INDEX ix_product_light ON product (light);
CREATE INDEX ix_product_pet_safe ON product (pet_safe);
