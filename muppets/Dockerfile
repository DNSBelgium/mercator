FROM node:lts-alpine@sha256:09faa7dabeae557cb6baff17be5b216fc4e3c9608aa04fe71695aad3d229a9c7

RUN apk add -u --update-cache \
    libstdc++ \
    chromium \
    harfbuzz \
    nss \
    ttf-freefont \
    font-noto-emoji \
    # CVE-2023-0286
    && apk upgrade --no-cache libssl1.1 libcrypto1.1 \
    && rm -rf /var/cache/* \
    && mkdir /var/cache/apk
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium-browser \
    CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/

#ARG NODE_ENV=production
#ENV NODE_ENV $NODE_ENV

WORKDIR /app
RUN chown node:node ./
USER node

ADD package.json package-lock.json tsconfig.json /app/
RUN npm ci && npm cache clean --force

ADD src /app/src

RUN npm run build

CMD ["node", "--trace-deprecation", "/app/dist/server.js"]
