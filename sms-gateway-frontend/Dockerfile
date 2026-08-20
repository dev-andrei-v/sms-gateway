# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files
COPY package*.json ./

# Don't use npm ci if lockfile might be out of sync
RUN npm install

# Copy source code
COPY . .

# Build application
RUN npm run build

# Production stage with Nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx files
RUN rm -rf ./*

# Copy built files from build stage
COPY --from=build /app/dist .

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Add healthcheck script
#RUN echo '#!/bin/sh' > /healthcheck.sh && \
#    echo 'wget --no-verbose --tries=1 --spider http://localhost/ || exit 1' >> /healthcheck.sh && \
#    chmod +x /healthcheck.sh

# Expose port
EXPOSE 80

# Health check
#HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
#  CMD ["/healthcheck.sh"]

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
