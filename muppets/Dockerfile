FROM node:lts-alpine@sha256:b375b98d1dcd56f5783efdd80a4d6ff5a0d6f3ce7921ec99c17851db6cba2a93

RUN apk add -u --update-cache \
    libstdc++ \
    chromium \
    harfbuzz \
    nss \
    ttf-freefont \
    font-noto-emoji \
    && rm -rf /var/cache/* \
    && mkdir /var/cache/apk

RUN \
    # CVE-2022-30065 \
    apk upgrade --no-cache busybox && \
    # CVE-2022-37434 \
    apk upgrade --no-cache zlib

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
