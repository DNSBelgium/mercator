#FROM node:lts-alpine@sha256:777b5a7bf0c40e37766ff8df382c900f16c688ed05ae3a72d32a26f3e9002cf9 as build
FROM node:lts-alpine as build

#ARG NODE_ENV=production
#ENV NODE_ENV $NODE_ENV

WORKDIR /app

COPY package.json npm-shrinkwrap.json /app/
RUN npm ci --force && npm cache clean --force

ADD tsconfig.json /app
ADD src /app/src
ADD public /app/public

RUN npm run build --force

FROM nginx:stable-alpine

COPY --from=build /app/build/ /usr/share/nginx/html

RUN apk upgrade --update-cache \
      # CVE-2022-32207
      curl libcurl \
      # CVE-2022-2097 \
      libcrypto1.1 libssl1.1 \
      --no-cache

## Nginx config
# Port 8080
# React router config
COPY nginx/nginx.conf /etc/nginx/templates/default.conf.template

WORKDIR /usr/share/nginx/html
COPY .env .
COPY ./env.sh /docker-entrypoint.d/99-env.sh
