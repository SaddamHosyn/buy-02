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

# Ensure we use a stable Java version (17 or 21) if available
if [ -x "/usr/libexec/java_home" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || /usr/libexec/java_home -v 21 2>/dev/null || echo $JAVA_HOME)
    export PATH="$JAVA_HOME/bin:$PATH"
    echo -e "${GREEN}Using Java Version:${NC}"
    java -version 2>&1 | head -n 1
fi

# Ensure logs and pids directories exist
mkdir -p "$PROJECT_ROOT/logs"
mkdir -p "$PROJECT_ROOT/pids"

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   Buy-01 Application Startup${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Function to wait for a service to be ready on a port
wait_for_port() {
    local port=$1
    local service_name=$2
    local max_wait=${3:-60}
    local count=0
    echo -e "${YELLOW}  Waiting for $service_name to be ready on port $port...${NC}"
    while ! nc -z localhost $port 2>/dev/null; do
        sleep 2
        count=$((count + 2))
        if [ $count -ge $max_wait ]; then
            echo -e "${RED}  ✗ $service_name failed to start within ${max_wait}s${NC}"
            return 1
        fi
    done
    echo -e "${GREEN}  ✓ $service_name is ready${NC}"
    return 0
}

# Check if MongoDB is running
echo -e "${YELLOW}[1/8] Checking MongoDB...${NC}"
if ! pgrep -x "mongod" > /dev/null; then
    echo -e "${YELLOW}MongoDB is not running. Starting it via brew...${NC}"
    brew services start mongodb-community
    sleep 2
fi
echo -e "${GREEN}✓ MongoDB is running${NC}"

# Check if Kafka is running (Kafka 4.x uses KRaft mode - no Zookeeper needed)
echo -e "${YELLOW}[2/8] Checking Kafka...${NC}"
if ! pgrep -f "kafka.Kafka\|kafka.server.KafkaServer\|org.apache.kafka" > /dev/null; then
    echo -e "${YELLOW}Kafka is not running. Starting it via brew...${NC}"
    brew services start kafka
    sleep 8
fi
echo -e "${GREEN}✓ Kafka check complete${NC}"
echo ""

# Start Service Registry (Eureka)
echo -e "${YELLOW}[3/8] Starting Service Registry (Eureka)...${NC}"
cd "$PROJECT_ROOT/service-registry"
mvn spring-boot:run > "$PROJECT_ROOT/logs/service-registry.log" 2>&1 &
SERVICE_REGISTRY_PID=$!
echo "$SERVICE_REGISTRY_PID" > "$PROJECT_ROOT/pids/service-registry.pid"
echo -e "${GREEN}✓ Service Registry started (PID: $SERVICE_REGISTRY_PID)${NC}"
echo -e "${GREEN}  URL: http://localhost:8761${NC}"
wait_for_port 8761 "Service Registry" 90
echo ""

# Start User Service
echo -e "${YELLOW}[4/8] Starting User Service...${NC}"
cd "$PROJECT_ROOT/user-service"
mvn spring-boot:run > "$PROJECT_ROOT/logs/user-service.log" 2>&1 &
USER_SERVICE_PID=$!
echo "$USER_SERVICE_PID" > "$PROJECT_ROOT/pids/user-service.pid"
echo -e "${GREEN}✓ User Service started (PID: $USER_SERVICE_PID)${NC}"
echo -e "${GREEN}  Port: 8081${NC}"
echo ""

# Start Product Service
echo -e "${YELLOW}[5/8] Starting Product Service...${NC}"
cd "$PROJECT_ROOT/product-service"
mvn spring-boot:run > "$PROJECT_ROOT/logs/product-service.log" 2>&1 &
PRODUCT_SERVICE_PID=$!
echo "$PRODUCT_SERVICE_PID" > "$PROJECT_ROOT/pids/product-service.pid"
echo -e "${GREEN}✓ Product Service started (PID: $PRODUCT_SERVICE_PID)${NC}"
echo -e "${GREEN}  Port: 8082${NC}"
echo ""

# Start Media Service
echo -e "${YELLOW}[6/8] Starting Media Service...${NC}"
cd "$PROJECT_ROOT/media-service"
mvn spring-boot:run > "$PROJECT_ROOT/logs/media-service.log" 2>&1 &
MEDIA_SERVICE_PID=$!
echo "$MEDIA_SERVICE_PID" > "$PROJECT_ROOT/pids/media-service.pid"
echo -e "${GREEN}✓ Media Service started (PID: $MEDIA_SERVICE_PID)${NC}"
echo -e "${GREEN}  Port: 8083${NC}"
echo ""

# Start Order Service
echo -e "${YELLOW}[7/8] Starting Order Service...${NC}"
cd "$PROJECT_ROOT/order-service"
mvn spring-boot:run > "$PROJECT_ROOT/logs/order-service.log" 2>&1 &
ORDER_SERVICE_PID=$!
echo "$ORDER_SERVICE_PID" > "$PROJECT_ROOT/pids/order-service.pid"
echo -e "${GREEN}✓ Order Service started (PID: $ORDER_SERVICE_PID)${NC}"
echo -e "${GREEN}  Port: 8084${NC}"
echo ""

# Wait for microservices to be ready
echo -e "${YELLOW}Waiting for microservices to start...${NC}"
wait_for_port 8081 "User Service" 60
wait_for_port 8082 "Product Service" 60
wait_for_port 8083 "Media Service" 60
wait_for_port 8084 "Order Service" 60
echo ""

# Start API Gateway
echo -e "${YELLOW}[8/8] Starting API Gateway...${NC}"
cd "$PROJECT_ROOT/api-gateway"
mvn spring-boot:run > "$PROJECT_ROOT/logs/api-gateway.log" 2>&1 &
API_GATEWAY_PID=$!
echo "$API_GATEWAY_PID" > "$PROJECT_ROOT/pids/api-gateway.pid"
echo -e "${GREEN}✓ API Gateway started (PID: $API_GATEWAY_PID)${NC}"
echo -e "${GREEN}  URL: http://localhost:8090${NC}"
wait_for_port 8090 "API Gateway" 60
echo ""

# Start Frontend (Angular)
echo -e "${YELLOW}[9/9] Starting Frontend (Angular)...${NC}"
cd "$PROJECT_ROOT/buy-01-ui"
# Install dependencies if node_modules doesn't exist or package-lock changed
if [ ! -d "node_modules" ] || [ "package.json" -nt "node_modules" ]; then
    echo -e "${YELLOW}  Installing npm dependencies...${NC}"
    npm install > "$PROJECT_ROOT/logs/frontend-install.log" 2>&1
    if [ $? -ne 0 ]; then
        echo -e "${RED}  ✗ npm install failed. Check logs/frontend-install.log${NC}"
    fi
fi
npm start > "$PROJECT_ROOT/logs/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "$FRONTEND_PID" > "$PROJECT_ROOT/pids/frontend.pid"
echo -e "${GREEN}✓ Frontend started (PID: $FRONTEND_PID)${NC}"
wait_for_port 4200 "Frontend" 120
echo -e "${GREEN}  URL: http://localhost:4200${NC}"
echo ""

# Summary
echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}✓ All services started successfully!${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "${BLUE}Service URLs:${NC}"
echo -e "  Frontend:         ${GREEN}http://localhost:4200${NC}"
echo -e "  API Gateway:      ${GREEN}http://localhost:8090${NC}"
echo -e "  Service Registry: ${GREEN}http://localhost:8761${NC}"
echo -e "  User Service:     ${GREEN}http://localhost:8081${NC}"
echo -e "  Product Service:  ${GREEN}http://localhost:8082${NC}"
echo -e "  Media Service:    ${GREEN}http://localhost:8083${NC}"
echo -e "  Order Service:    ${GREEN}http://localhost:8084${NC}"
echo ""
echo -e "${YELLOW}Logs are saved in: $PROJECT_ROOT/logs/${NC}"
echo -e "${YELLOW}Process IDs are saved in: $PROJECT_ROOT/pids/${NC}"
echo ""
echo -e "${RED}To stop all services, run: ./stop_all.sh${NC}"
echo ""
