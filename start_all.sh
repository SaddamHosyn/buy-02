#!/bin/bash

#############################################################
# Integrated Application Startup Script
# Starts all backend microservices + frontend in order
#############################################################

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   Buy-01 Application Startup${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Check if MongoDB is running
echo -e "${YELLOW}[1/7] Checking MongoDB...${NC}"
#if ! pgrep -x "mongod" > /dev/null; then
 #   echo -e "${RED}❌ MongoDB is not running!${NC}"
 #   echo -e "${YELLOW}Please start MongoDB first:${NC}"
  
  #  echo -e "  brew services start mongodb-community"
   # echo -e "  OR"
   # echo -e "  mongod --config /opt/homebrew/etc/mongod.conf"
   # exit 1
#fi
echo -e "${GREEN}✓ MongoDB is running${NC}"
echo ""

# Start Service Registry (Eureka)
echo -e "${YELLOW}[2/7] Starting Service Registry (Eureka)...${NC}"
cd "$PROJECT_ROOT/service-registry"
mvn spring-boot:run > "$PROJECT_ROOT/logs/service-registry.log" 2>&1 &
SERVICE_REGISTRY_PID=$!
echo "$SERVICE_REGISTRY_PID" > "$PROJECT_ROOT/pids/service-registry.pid"
echo -e "${GREEN}✓ Service Registry started (PID: $SERVICE_REGISTRY_PID)${NC}"
echo -e "${GREEN}  URL: http://localhost:8761${NC}"
echo ""

# Wait for Service Registry to be ready
echo -e "${YELLOW}Waiting for Service Registry to be ready...${NC}"
sleep 20
echo ""

# Start User Service
echo -e "${YELLOW}[3/7] Starting User Service...${NC}"
cd "$PROJECT_ROOT/user-service"
mvn spring-boot:run > "$PROJECT_ROOT/logs/user-service.log" 2>&1 &
USER_SERVICE_PID=$!
echo "$USER_SERVICE_PID" > "$PROJECT_ROOT/pids/user-service.pid"
echo -e "${GREEN}✓ User Service started (PID: $USER_SERVICE_PID)${NC}"
echo -e "${GREEN}  Port: 8081${NC}"
echo ""

# Start Product Service
echo -e "${YELLOW}[4/7] Starting Product Service...${NC}"
cd "$PROJECT_ROOT/product-service"
mvn spring-boot:run > "$PROJECT_ROOT/logs/product-service.log" 2>&1 &
PRODUCT_SERVICE_PID=$!
echo "$PRODUCT_SERVICE_PID" > "$PROJECT_ROOT/pids/product-service.pid"
echo -e "${GREEN}✓ Product Service started (PID: $PRODUCT_SERVICE_PID)${NC}"
echo -e "${GREEN}  Port: 8082${NC}"
echo ""

# Start Media Service
echo -e "${YELLOW}[5/7] Starting Media Service...${NC}"
cd "$PROJECT_ROOT/media-service"
mvn spring-boot:run > "$PROJECT_ROOT/logs/media-service.log" 2>&1 &
MEDIA_SERVICE_PID=$!
echo "$MEDIA_SERVICE_PID" > "$PROJECT_ROOT/pids/media-service.pid"
echo -e "${GREEN}✓ Media Service started (PID: $MEDIA_SERVICE_PID)${NC}"
echo -e "${GREEN}  Port: 8083${NC}"
echo ""

# Wait for microservices to register
echo -e "${YELLOW}Waiting for services to register with Eureka...${NC}"
sleep 15
echo ""

# Start API Gateway
echo -e "${YELLOW}[6/7] Starting API Gateway...${NC}"
cd "$PROJECT_ROOT/api-gateway"
mvn spring-boot:run > "$PROJECT_ROOT/logs/api-gateway.log" 2>&1 &
API_GATEWAY_PID=$!
echo "$API_GATEWAY_PID" > "$PROJECT_ROOT/pids/api-gateway.pid"
echo -e "${GREEN}✓ API Gateway started (PID: $API_GATEWAY_PID)${NC}"
echo -e "${GREEN}  URL: http://localhost:8080${NC}"
echo ""

# Wait for API Gateway to be ready
echo -e "${YELLOW}Waiting for API Gateway to be ready...${NC}"
sleep 10
echo ""

# Start Frontend (Angular)
echo -e "${YELLOW}[7/7] Starting Frontend (Angular)...${NC}"
cd "$PROJECT_ROOT/buy-01-ui"
npm start > "$PROJECT_ROOT/logs/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "$FRONTEND_PID" > "$PROJECT_ROOT/pids/frontend.pid"
echo -e "${GREEN}✓ Frontend started (PID: $FRONTEND_PID)${NC}"
echo -e "${GREEN}  URL: http://localhost:4200${NC}"
echo ""

# Summary
echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}✓ All services started successfully!${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "${BLUE}Service URLs:${NC}"
echo -e "  Frontend:         ${GREEN}http://localhost:4200${NC}"
echo -e "  API Gateway:      ${GREEN}http://localhost:8080${NC}"
echo -e "  Service Registry: ${GREEN}http://localhost:8761${NC}"
echo -e "  User Service:     ${GREEN}http://localhost:8081${NC}"
echo -e "  Product Service:  ${GREEN}http://localhost:8082${NC}"
echo -e "  Media Service:    ${GREEN}http://localhost:8083${NC}"
echo ""
echo -e "${YELLOW}Logs are saved in: $PROJECT_ROOT/logs/${NC}"
echo -e "${YELLOW}Process IDs are saved in: $PROJECT_ROOT/pids/${NC}"
echo ""
echo -e "${RED}To stop all services, run: ./stop_all.sh${NC}"
echo ""
