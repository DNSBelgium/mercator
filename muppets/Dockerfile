FROM node:lts-alpine

RUN apk add -u --update-cache \
    libstdc++ \
    chromium \
    harfbuzz \
    nss \
    ttf-freefont \
    font-noto-emoji \
    # CVE-2022-2740 \
    freetype \
    # CVE-2021-44648
    gdk-pixbuf \
    # CVE-2022-0891
    tiff-dev \
    # CVE-2022-23308 and more
    curl libxml2 libcrypto1.1 libssl1.1 libxslt \
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

ADD package.json package-lock.json /app/
RUN npm ci && npm cache clean --force

ADD tsconfig.json /app
ADD src /app/src

RUN npm run build

ENV S3_ENDPOINT "UNDEFINED"
ENV S3_BUCKET "UNDEFINED"
ENV SQS_ENDPOINT "UNDEFINED"
ENV SQS_INPUT_QUEUE "UNDEFINED"
ENV SQS_OUTPUT_QUEUE "UNDEFINED"
ENV SERVER_PORT "UNDEFINED"

ADD entrypoint.sh /app/
ADD src/config/config.json /app/dist/config.json

ENTRYPOINT ["./entrypoint.sh"]
CMD ["node", "/app/dist/server.js"]
