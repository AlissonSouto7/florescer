# syntax=docker/dockerfile:1

# --- build ---------------------------------------------------------------
# Usa o Maven Wrapper versionado no repo: a versão do Maven é a mesma aqui,
# na máquina do dev e no CI.
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build

# Copiar só o descritor primeiro faz o Docker reaproveitar o cache de dependências
# enquanto o pom não muda, mesmo que o código mude.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package

# --- runtime -------------------------------------------------------------
# JRE (não JDK) reduz superfície de ataque e tamanho da imagem.
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Nunca roda como root.
RUN addgroup --system florescer && adduser --system --ingroup florescer florescer

COPY --from=build --chown=florescer:florescer /build/target/*.jar app.jar

# Imagens de produto ficam num volume: container é descartável, upload não.
RUN mkdir -p /app/uploads && chown florescer:florescer /app/uploads
VOLUME ["/app/uploads"]

USER florescer
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
