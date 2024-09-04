#FROM node:20-alpine3.20
FROM node@sha256:1a526b97cace6b4006256570efa1a29cd1fe4b96a5301f8d48e87c5139438a45

RUN apk add -u --update-cache \
    libstdc++ \
    chromium \
    harfbuzz \
    nss \
    ttf-freefont \
    font-noto-emoji \
    openssl \
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
