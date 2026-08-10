-- Estado inicial do schema, correspondente ao que o Hibernate vinha criando
-- com ddl-auto: update. A partir daqui o schema passa a ser versionado e o
-- Hibernate apenas valida, sem alterar nada por conta própria.
--
-- Duas diferenças deliberadas em relação ao que existia:
--
-- price era float8, porque a entidade usava Double. Ponto flutuante binário não
-- representa valores decimais exatamente, então somar itens acumula erro e
-- comparar por igualdade deixa de ser confiável. numeric(10,2) guarda o valor
-- exato, e 10 dígitos com 2 casas cobrem até 99.999.999,99.
--
-- As colunas obrigatórias passam a ser NOT NULL de fato. Antes só image_path
-- era, e os limites de tamanho prometidos pelos DTOs (500 caracteres em
-- description e care_requirements) não existiam no banco: passar de 255 virava
-- erro de gravação e resposta 500 em vez de 400.

CREATE TABLE product (
    id                UUID           NOT NULL,
    name              VARCHAR(255)   NOT NULL,
    type              VARCHAR(100)   NOT NULL,
    description       VARCHAR(500)   NOT NULL,
    price             NUMERIC(10, 2) NOT NULL,
    quantity_stock    INTEGER        NOT NULL,
    care_requirements VARCHAR(500)   NOT NULL,
    availability      BOOLEAN        NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    image_path        VARCHAR(255)   NOT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,

    CONSTRAINT pk_product PRIMARY KEY (id),
    -- O enum é validado no banco também: a aplicação pode mudar, os dados ficam.
    CONSTRAINT ck_product_status CHECK (status IN ('ATIVO', 'INATIVO', 'ESGOTADO')),
    -- As mesmas regras que a validação da API aplica, agora garantidas onde os
    -- dados de fato moram. Validação da aplicação protege contra entrada errada;
    -- constraint protege contra qualquer caminho que chegue ao banco.
    CONSTRAINT ck_product_price CHECK (price > 0),
    CONSTRAINT ck_product_stock CHECK (quantity_stock >= 0)
);

-- A listagem ordena por nome por padrão e permite ordenar por preço e status.
-- Sem índice, cada página faz varredura completa e ordenação em memória.
CREATE INDEX ix_product_name ON product (name);
CREATE INDEX ix_product_status ON product (status);
