# syntax=docker/dockerfile:1

# --- dependências --------------------------------------------------------
# Estágio próprio para as dependências: enquanto o package-lock não mudar, o
# Docker reaproveita esta camada mesmo que todo o código mude.
FROM node:22-alpine AS deps
WORKDIR /app

COPY package.json package-lock.json ./
# `npm ci` instala exatamente o que está no lock, sem resolver versões de novo.
RUN npm ci

# --- build ---------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Os endereços das APIs entram como argumento porque o Next embute as variáveis
# NEXT_PUBLIC_ no JavaScript que vai para o navegador, durante o build. Passá-las
# só em runtime não teria efeito: o valor já estaria compilado.
ARG NEXT_PUBLIC_PRODUCT_API=http://localhost:8081
ARG NEXT_PUBLIC_AUTH_API=http://localhost:8080
ARG NEXT_PUBLIC_WHATSAPP=
# O endereco interno tambem entra no build: o next.config o usa para autorizar
# de quais hosts o otimizador de imagem pode carregar, e essa lista e resolvida
# na hora do build.
ARG PRODUCT_API=http://localhost:8081
ENV NEXT_PUBLIC_PRODUCT_API=$NEXT_PUBLIC_PRODUCT_API \
    NEXT_PUBLIC_AUTH_API=$NEXT_PUBLIC_AUTH_API \
    NEXT_PUBLIC_WHATSAPP=$NEXT_PUBLIC_WHATSAPP \
    PRODUCT_API=$PRODUCT_API \
    NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# --- runtime -------------------------------------------------------------
FROM node:22-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000

# Nunca roda como root.
RUN addgroup --system --gid 1001 florescer \
 && adduser --system --uid 1001 --ingroup florescer florescer

# O build standalone traz só o necessário para executar, sem node_modules
# inteiro. Static e public ficam de fora dele e precisam ser copiados à parte.
COPY --from=build --chown=florescer:florescer /app/.next/standalone ./
COPY --from=build --chown=florescer:florescer /app/.next/static ./.next/static
COPY --from=build --chown=florescer:florescer /app/public ./public

USER florescer
EXPOSE 3000

ENTRYPOINT ["node", "server.js"]
