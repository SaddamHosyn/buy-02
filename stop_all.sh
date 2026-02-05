#!/bin/bash

#############################################################
# Stop All Services Script
#############################################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
PIDS_DIR="$PROJECT_ROOT/pids"

echo -e "${RED}============================================${NC}"
echo -e "${RED}   Stopping All Services${NC}"
echo -e "${RED}============================================${NC}"
echo ""

# Function to stop a service
stop_service() {
    local service_name=$1
    local port=$2
    local pid_file="$PIDS_DIR/${service_name}.pid"
    
    echo -e "${YELLOW}Stopping $service_name...${NC}"

    # 1. Try stopping via PID file if it exists
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            pkill -P $pid > /dev/null 2>&1
            kill $pid > /dev/null 2>&1
            sleep 1
        fi
        rm "$pid_file"
    fi

    # 2. Aggressive cleanup: Kill any Java process on the specific port
    if [ ! -z "$port" ]; then
        local port_pid=$(lsof -t -i:$port)
        if [ ! -z "$port_pid" ]; then
            echo -e "${YELLOW}  Cleaning up ghost process on port $port (PID: $port_pid)...${NC}"
            kill -9 $port_pid > /dev/null 2>&1
        fi
    fi
    echo -e "${GREEN}✓ $service_name cleaned up${NC}"
}

# Stop services in reverse order
stop_service "frontend" 4200
stop_service "api-gateway" 8090
stop_service "order-service" 8084
stop_service "media-service" 8083
stop_service "product-service" 8082
stop_service "user-service" 8081
stop_service "service-registry" 8761

echo ""
echo -e "${GREEN}✓ All services stopped${NC}"
echo ""
