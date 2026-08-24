# syntax=docker/dockerfile:1

# --- build ---------------------------------------------------------------
# Usa o Maven Wrapper versionado no repo: a versão do Maven é a mesma aqui,
# na máquina do dev e no CI.
# Noble, e não Alpine: as imagens Alpine do Temurin existem **apenas para
# amd64**, e o build multiplataforma falha com "no match for platform in
# manifest" antes mesmo de compilar. Medido no manifesto:
#
#   17-jdk-alpine  linux/amd64
#   17-jdk-noble   linux/amd64, linux/arm, linux/arm64, ...
#
# Custa 25 MB a mais na imagem final, e compra a possibilidade de rodar em
# máquina ARM, que é o que as nuvens oferecem de graça.
FROM eclipse-temurin:24-jdk-noble AS build
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
FROM eclipse-temurin:24-jre-noble AS runtime
WORKDIR /app

# Nunca roda como root.
RUN addgroup --system florescer && adduser --system --ingroup florescer florescer

COPY --from=build --chown=florescer:florescer /build/target/*.jar app.jar

USER florescer
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
