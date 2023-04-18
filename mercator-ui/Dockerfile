FROM node:lts-alpine@sha256:a136ed7b0df71082cdb171f36d640ea3b392a5c70401c642326acee767b8c540 as build

#ARG NODE_ENV=production
#ENV NODE_ENV $NODE_ENV

WORKDIR /app

COPY package.json npm-shrinkwrap.json /app/

# see https://github.com/webpack/webpack/issues/14532
RUN NODE_OPTIONS="--openssl-legacy-provider" npm ci --force && npm cache clean --force

ADD tsconfig.json /app
ADD src /app/src
ADD public /app/public

RUN NODE_OPTIONS="--openssl-legacy-provider" npm run build --force

#FROM nginx:stable-alpine
FROM nginx@sha256:26a1b20f194c2adab3be2d9e569ca837ddd31fe105d95dd51344e5bf829f1349

COPY --from=build /app/build/ /usr/share/nginx/html

## Nginx config
# Port 8080
# React router config
COPY nginx/nginx.conf /etc/nginx/templates/default.conf.template

WORKDIR /usr/share/nginx/html
COPY .env .
COPY ./env.sh /docker-entrypoint.d/99-env.sh
