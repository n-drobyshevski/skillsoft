FROM node:20-alpine

WORKDIR /app

# Install dependencies first
COPY package*.json ./
RUN npm ci

# Copy configuration files
COPY postcss.config.mjs ./
COPY next.config.ts ./
COPY tsconfig*.json ./
COPY biome.json ./
COPY components.json ./

# Copy source code
COPY . .

# Expose port
EXPOSE 3000

# Start development server with hot reload
CMD ["npm", "run", "dev"]
