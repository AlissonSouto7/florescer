-- Estado inicial do schema, correspondente ao que o Hibernate vinha criando
-- com ddl-auto: update. A partir daqui o schema é versionado e o Hibernate
-- apenas valida.
--
-- Uma diferença deliberada: email ganha NOT NULL e UNIQUE.
--
-- A unicidade existia apenas como um findByEmail antes do insert, o que deixa
-- uma janela entre a consulta e a gravação: duas requisições simultâneas com o
-- mesmo endereço passam as duas pela verificação e criam duas contas, e a
-- partir daí o login fica indefinido. Só o banco consegue garantir isso, porque
-- só ele vê as duas transações.

CREATE TABLE tb_roles (
    role_id BIGINT       NOT NULL AUTO_INCREMENT,
    name    VARCHAR(50)  NOT NULL,

    CONSTRAINT pk_roles PRIMARY KEY (role_id),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE = InnoDB;

CREATE TABLE tb_users (
    user_id  BINARY(16)   NOT NULL,
    name     VARCHAR(50)  NOT NULL,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(60)  NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (user_id),
    -- Fecha a corrida no registro concorrente.
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE tb_users_roles (
    user_id BINARY(16) NOT NULL,
    role_id BIGINT     NOT NULL,

    CONSTRAINT pk_users_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_users_roles_user FOREIGN KEY (user_id) REFERENCES tb_users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_users_roles_role FOREIGN KEY (role_id) REFERENCES tb_roles (role_id)
) ENGINE = InnoDB;

-- Papéis iniciais. Substitui o data.sql, que tinha dois INSERT sem ponto e
-- vírgula e funcionava por acidente do separador de fallback do Spring: bastava
-- alguém adicionar um comando multilinha para o boot quebrar.
--
-- Os IDs são explícitos porque Role.Values os declara fixos no código, e o
-- vínculo entre os dois precisa valer.
INSERT INTO tb_roles (role_id, name) VALUES (1, 'ADMIN');
INSERT INTO tb_roles (role_id, name) VALUES (2, 'BASIC');
