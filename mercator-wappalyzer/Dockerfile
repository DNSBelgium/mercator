FROM node:lts-alpine

RUN apk add -u --no-cache \
    libstdc++ \
    chromium \
    harfbuzz \
    nss \
    freetype \
    ttf-freefont \
    font-noto-emoji \
     # CVE-2021-44648
    gdk-pixbuf \
    # CVE-2022-0891
    tiff-dev \
    && rm -rf /var/cache/* \
    && mkdir /var/cache/apk

RUN apk upgrade --update-cache \
      # CVE-2022-23308 and more
      curl libxml2 libcrypto1.1 libssl1.1 libxslt \
      --no-cache

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

RUN npm run build

CMD ["node", "/app/dist/index.js"]
