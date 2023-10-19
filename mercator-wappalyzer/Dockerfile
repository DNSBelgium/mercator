FROM node:lts-alpine@sha256:51490771aba658439d29b1b03b60fc31e67bf0da3e01cb5903716310df4be1c1

RUN apk add -u --no-cache \
    libstdc++ \
    chromium \
    harfbuzz \
    nss \
    freetype \
    ttf-freefont \
    font-noto-emoji

ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium-browser \
    CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/

#ARG NODE_ENV=production
#ENV NODE_ENV $NODE_ENV

WORKDIR /app
RUN chown node:node ./
USER node

ADD package.json package-lock.json /app/

RUN npm ci && npm cache clean --force

ADD tsconfig.json /app
ADD src /app/src
ADD .env /app/.env

# RUN npm run build --omit=dev
RUN npm run build

CMD ["node", "/app/dist/index.js"]
