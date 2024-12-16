FROM node:20-alpine3.20 AS build

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

# stable-alpine
FROM nginx@sha256:35e3238f2f0925a505d5d697df9a9148db9a0c78e89fd2e253919047b3cec824

COPY --from=build /app/build/ /usr/share/nginx/html

## Nginx config
# Port 8080
# React router config
COPY nginx/nginx.conf /etc/nginx/templates/default.conf.template

WORKDIR /usr/share/nginx/html
COPY .env .
COPY ./env.sh /docker-entrypoint.d/99-env.sh
