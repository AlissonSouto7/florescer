# 🌸 Florescer - API REST para Gerenciamento de Produtos

Bem-vindo(a) à **Florescer**, uma API REST desenvolvida como projeto de portfólio para demonstrar habilidades como desenvolvedor Java Júnior. O projeto simula o cadastro, listagem, edição, exclusão e upload de imagens de produtos.

---

## 🚀 Tecnologias Utilizadas

- Java 17
- Spring Boot
- Spring Data JPA
- Hibernate
- Bean Validation
- Swagger (OpenAPI)
- PostgreSQL (ou H2 para testes)
- Lombok
- UUID
- Upload de imagens via sistema de arquivos

---

## 📂 Funcionalidades

- ✅ Cadastro de produtos
- ✅ Listagem de todos os produtos
- ✅ Busca por nome ou ID
- ✅ Atualização de produtos
- ✅ Exclusão de produtos
- ✅ Upload de imagem e recuperação por URL
- ✅ Validação com mensagens customizadas
- ✅ Documentação com Swagger UI
- ✅ Tratamento global de exceções

---

## 🔧 Como Rodar o Projeto Localmente

### Pré-requisitos

- Java 17+
- Maven 3.8+
- PostgreSQL (ou utilize H2 para testes)
- Git

### Passos

```bash
# Clone o repositório
git clone https://github.com/AlissonSouto7/florescer.git
cd florescer

# Rode o projeto
./mvnw spring-boot:run
