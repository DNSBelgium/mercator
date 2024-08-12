FROM golang:1.22.5-alpine AS gobuilder
RUN apk add git
RUN git clone https://github.com/editicalu/gowap ./gowap && cd gowap && go build -o /gowap_bin cmd/gowap/main.go

FROM node:20-alpine3.20
RUN apk add -u --no-cache \
    libstdc++ \
    chromium \
    harfbuzz \
    nss \
    freetype \
    ttf-freefont \
    font-noto-emoji

WORKDIR /app
RUN chown node:node ./
USER node

ADD package.json package-lock.json /app/

RUN npm ci && npm cache clean --force

ADD tsconfig.json /app
ADD src /app/src
ADD .env /app/.env

COPY --from=gobuilder /gowap_bin /app/gowap
ENV GOWAP_PATH="/app/gowap"
# RUN npm run build --omit=dev
RUN npm run build

ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium-browser \
    CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/

CMD ["node", "/app/dist/index.js"]
