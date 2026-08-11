# syntax=docker/dockerfile:1

# --- build ---------------------------------------------------------------
# Usa o Maven Wrapper versionado no repo: a versão do Maven é a mesma aqui,
# na máquina do dev e no CI.
FROM eclipse-temurin:25-jdk-alpine AS build
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
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Nunca roda como root.
RUN addgroup --system florescer && adduser --system --ingroup florescer florescer

COPY --from=build --chown=florescer:florescer /build/target/*.jar app.jar

# Imagens de produto ficam num volume: container é descartável, upload não.
#
# O diretório precisa existir na imagem, com o dono certo, ANTES de o volume ser
# montado. Quando o Docker monta um volume nomeado num caminho que não existe na
# imagem, ele cria o ponto de montagem como root:root, e o processo, que roda
# como florescer, não consegue gravar. Existindo antes, o Docker herda dono e
# permissão daqui.
#
# Medido: sem isto, o upload responde 400 "Erro ao salvar imagem" e o log mostra
# a falha de escrita no caminho do volume.
RUN mkdir -p /var/florescer/uploads && chown -R florescer:florescer /var/florescer
VOLUME ["/var/florescer/uploads"]

USER florescer
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
