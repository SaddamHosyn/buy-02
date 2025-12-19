# Stage 1: Build the Angular application
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install --legacy-peer-deps

# Copy source code
COPY . .

# Build the application
RUN npm run build -- --configuration production

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Copy built files from build stage
COPY --from=build /app/dist/buy-01-ui/browser /usr/share/nginx/html

# Copy nginx HTTPS configuration
COPY nginx-https.conf /etc/nginx/conf.d/default.conf

# Copy SSL certificates
COPY certs /etc/nginx/certs

# Expose ports
EXPOSE 80
EXPOSE 443

CMD ["nginx", "-g", "daemon off;"]
