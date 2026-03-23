FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx tsc -b && npx vite build --outDir dist

FROM node:20-alpine
WORKDIR /app
RUN npm init -y && npm install express http-proxy-middleware
COPY --from=build /app/dist ./dist
COPY server.js .
ENV API_TARGET=http://app:8080
EXPOSE 5173
CMD ["node", "server.js"]
