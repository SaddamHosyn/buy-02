#!/bin/bash

#############################################################
# Docker Application Startup Script
# Starts all services in Docker with proper sequencing
#############################################################

# Exit immediately if a command exits with a non-zero status.
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   Buy-01 Docker Startup${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Build all Spring Boot microservices JARs
echo -e "${YELLOW}Building all microservices...${NC}"
for service in service-registry user-service product-service media-service api-gateway; do
  echo -e "${BLUE}--- Building $service ---${NC}"
  (cd $service && mvn clean package -DskipTests)
done
echo -e "${GREEN}✓ Build complete${NC}"
echo ""

# Build frontend
echo -e "${YELLOW}Building frontend...${NC}"
echo -e "${BLUE}--- Building buy-01-ui ---${NC}"
(cd buy-01-ui && npm install --legacy-peer-deps 2>/dev/null || true)
echo -e "${GREEN}✓ Frontend dependencies ready${NC}"
echo ""

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   Starting Services in Docker${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Step 1: Start infrastructure (MongoDB, Zookeeper, Kafka)
echo -e "${YELLOW}[1/7] Starting infrastructure (MongoDB, Zookeeper, Kafka)...${NC}"
docker-compose up -d mongodb zookeeper
sleep 5
docker-compose up -d kafka
echo -e "${GREEN}✓ Infrastructure started${NC}"
echo ""

# Step 2: Wait for Kafka to be ready
echo -e "${YELLOW}Waiting for Kafka to be ready...${NC}"
sleep 10
echo ""

# Step 3: Start Service Registry (Eureka)
echo -e "${YELLOW}[2/7] Starting Service Registry (Eureka)...${NC}"
docker-compose up --build -d service-registry
echo -e "${GREEN}✓ Service Registry started${NC}"
echo -e "${GREEN}  URL: http://localhost:8761${NC}"
echo ""

# Step 4: Wait for Service Registry to be ready
echo -e "${YELLOW}Waiting for Service Registry to be ready...${NC}"
sleep 20
echo ""

# Step 5: Start User Service
echo -e "${YELLOW}[3/7] Starting User Service...${NC}"
docker-compose up --build -d user-service
echo -e "${GREEN}✓ User Service started${NC}"
echo -e "${GREEN}  Port: 8081${NC}"
echo ""

# Step 6: Start Product Service
echo -e "${YELLOW}[4/7] Starting Product Service...${NC}"
docker-compose up --build -d product-service
echo -e "${GREEN}✓ Product Service started${NC}"
echo -e "${GREEN}  Port: 8082${NC}"
echo ""

# Step 7: Start Media Service
echo -e "${YELLOW}[5/7] Starting Media Service...${NC}"
docker-compose up --build -d media-service
echo -e "${GREEN}✓ Media Service started${NC}"
echo -e "${GREEN}  Port: 8083${NC}"
echo ""

# Step 8: Wait for microservices to register
echo -e "${YELLOW}Waiting for services to register with Eureka...${NC}"
sleep 15
echo ""

# Step 9: Start API Gateway
echo -e "${YELLOW}[6/7] Starting API Gateway...${NC}"
docker-compose up --build -d api-gateway
echo -e "${GREEN}✓ API Gateway started${NC}"
echo -e "${GREEN}  URL: https://localhost:8443${NC}"
echo ""

# Step 10: Wait for API Gateway to be ready
echo -e "${YELLOW}Waiting for API Gateway to be ready...${NC}"
sleep 10
echo ""

# Step 11: Start Frontend
echo -e "${YELLOW}[7/7] Starting Frontend (Angular in Docker)...${NC}"
docker-compose up --build -d frontend
echo -e "${GREEN}✓ Frontend started${NC}"
echo -e "${GREEN}  URL: http://localhost:4200${NC}"
echo ""

# Summary
echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}✓ All services started successfully in Docker!${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "${BLUE}Service URLs:${NC}"
echo -e "  Frontend:         ${GREEN}http://localhost:4200${NC}"
echo -e "  API Gateway:      ${GREEN}https://localhost:8443${NC}"
echo -e "  Service Registry: ${GREEN}http://localhost:8761${NC}"
echo -e "  User Service:     ${GREEN}http://localhost:8081${NC}"
echo -e "  Product Service:  ${GREEN}http://localhost:8082${NC}"
echo -e "  Media Service:    ${GREEN}http://localhost:8083${NC}"
echo ""
echo -e "${YELLOW}To view logs:${NC} docker-compose logs -f [service-name]"
echo -e "${YELLOW}To stop all services:${NC} docker-compose down"
echo -e "${YELLOW}To view all containers:${NC} docker ps"
echo ""
