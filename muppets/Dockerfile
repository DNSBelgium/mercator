FROM node:lts-alpine

RUN apk add -u --update-cache \
    libstdc++ \
    chromium \
    harfbuzz \
    nss \
    ttf-freefont \
    font-noto-emoji \
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

ENV S3_ENDPOINT "UNDEFINED"
ENV S3_BUCKET "UNDEFINED"
ENV SQS_ENDPOINT "UNDEFINED"
ENV SQS_INPUT_QUEUE "UNDEFINED"
ENV SQS_OUTPUT_QUEUE "UNDEFINED"
ENV SERVER_PORT "UNDEFINED"

CMD ["node", "--trace-deprecation", "/app/dist/server.js"]
