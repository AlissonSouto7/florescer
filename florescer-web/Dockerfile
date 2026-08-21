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

# Só os endereços INTERNOS entram no build, e é isso que torna a imagem a mesma
# em qualquer ambiente: são nomes de serviço da rede do Docker, iguais em dev,
# staging e produção. O endereço público não aparece em lugar nenhum, porque o
# navegador fala apenas com o domínio do próprio site.
#
# Eles precisam ser ARG, e não só variável de runtime: o Next resolve o destino
# dos rewrites durante o build. Passar só no `environment` do container não tem
# efeito nenhum, e a falha é silenciosa.
#
# O número do WhatsApp NÃO está aqui, nem no ambiente do container: ele é um
# dado da loja, guardado no banco e editado pela vendedora em
# /admin/configuracoes. A mesma imagem serve qualquer loja.
ARG PRODUCT_API=http://product-service:8081
ARG AUTH_API=http://auth-service:8080
ENV PRODUCT_API=$PRODUCT_API \
    AUTH_API=$AUTH_API \
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
