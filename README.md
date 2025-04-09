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
```

---

## 📄 Documentação da API

Acesse a documentação completa com Swagger em:

```
http://localhost:8080/swagger-ui.html
```

---

## 🧪 Exemplos de Requisição

### Criar Produto

```json
POST /products

{
  "name": "Camisa branca",
  "description": "Camisa de algodão com estampa floral",
  "price": 79.90
}
```

### Atualizar Produto

```json
PUT /products/{id}

{
  "name": "Camisa branca atualizada",
  "description": "Modelo slim com novos detalhes",
  "price": 89.90
}
```

---

## 📸 Upload de Imagem

**Endpoint:**
```http
POST /products/{id}/image
```

**Resposta:**
```json
{
  "imageUrl": "http://localhost:8080/images/{nomeDaImagem}.jpg"
}
```

---

## 🤖 Autor

Desenvolvido por **Alisson Souto**  
🔗 [LinkedIn](https://www.linkedin.com/in/alisson-souto-java/)

---

## 💡 Objetivo

Esse projeto foi criado como parte de meu portfólio para demonstrar habilidades práticas com Spring Boot e boas práticas de desenvolvimento. Estou em busca de oportunidades como desenvolvedor backend júnior. Entre em contato! 🚀
