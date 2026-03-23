FROM node:20-alpine
WORKDIR /app
COPY node_modules ./node_modules
COPY dist ./dist
COPY server.js .
ENV API_TARGET=http://app:8080
EXPOSE 5173
CMD ["node", "server.js"]
