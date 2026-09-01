# Stage 1: Build application
FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci

# Copy source and build client + server bundle
COPY . .
RUN npm run build

# Stage 2: Production runtime
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000

# Install production dependencies only
COPY package*.json ./
RUN npm ci --omit=dev

# Copy built assets
COPY --from=builder /app/dist ./dist

# Expose port (Render overrides with its own PORT env)
EXPOSE 3000

# Start server
CMD ["node", "dist/server.cjs"]
