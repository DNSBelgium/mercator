FROM golang:1.22.5-alpine AS gobuilder
RUN apk add git
RUN git clone https://github.com/editicalu/gowap ./gowap && cd gowap && go build -o /gowap_bin cmd/gowap/main.go

FROM node:lts-alpine@sha256:eb8101caae9ac02229bd64c024919fe3d4504ff7f329da79ca60a04db08cef52
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

COPY --from=gobuilder /gowap_bin /app/gowap
ENV GOWAP_PATH="/app/gowap"
# RUN npm run build --omit=dev
RUN npm run build
CMD ["node", "/app/dist/index.js"]
