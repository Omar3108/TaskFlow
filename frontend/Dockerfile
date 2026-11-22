# syntax=docker/dockerfile:1

# ---- Build stage: produce the static bundle ----
FROM node:26-alpine AS build
WORKDIR /app

# Dependencies first, so this layer is reused until package-lock.json changes.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Runtime stage: nginx serves the files and forwards /api to the backend ----
FROM nginx:1.28-alpine

# Files in /etc/nginx/templates are run through envsubst when the container starts, so the
# backend address (API_UPSTREAM) is configuration, not baked into the image.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

ENV API_UPSTREAM=http://api:8080
EXPOSE 80
