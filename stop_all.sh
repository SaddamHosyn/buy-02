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
            # Kill all child processes first (Java spawned by Maven)
            pkill -P $pid > /dev/null 2>&1
            kill $pid > /dev/null 2>&1
            sleep 1
            # Force kill if still running
            if ps -p $pid > /dev/null 2>&1; then
                kill -9 $pid > /dev/null 2>&1
            fi
        fi
        rm -f "$pid_file"
    fi

    # 2. Kill any process on the specific port
    if [ ! -z "$port" ]; then
        local port_pids=$(lsof -t -i:$port 2>/dev/null)
        if [ ! -z "$port_pids" ]; then
            echo -e "${YELLOW}  Cleaning up processes on port $port...${NC}"
            echo "$port_pids" | xargs kill -9 > /dev/null 2>&1
            sleep 1
        fi
    fi
    
    # 3. Verify port is free
    if [ ! -z "$port" ] && lsof -i:$port > /dev/null 2>&1; then
        echo -e "${RED}  ✗ Warning: Port $port may still be in use${NC}"
    else
        echo -e "${GREEN}✓ $service_name stopped${NC}"
    fi
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
