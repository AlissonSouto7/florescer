# syntax=docker/dockerfile:1

# O frontend é HTML, CSS e JS servidos como estão: não há passo de build, então
# não há estágio de compilação. Um nginx entregando arquivo estático basta, e é
# bem mais leve que um Node em runtime só para servir arquivo.
FROM nginx:1.27-alpine

# A imagem do nginx já traz uma página padrão; ela sai para não sobrar rota que
# não faz parte do produto.
RUN rm -rf /usr/share/nginx/html/*

COPY --chown=nginx:nginx . /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

# O Dockerfile não precisa ir junto com o site.
RUN rm -f /usr/share/nginx/html/Dockerfile /usr/share/nginx/html/nginx.conf

EXPOSE 80
