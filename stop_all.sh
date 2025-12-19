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
    local pid_file="$PIDS_DIR/${service_name}.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            echo -e "${YELLOW}Stopping $service_name (PID: $pid)...${NC}"
            kill $pid
            sleep 2
            
            # Force kill if still running
            if ps -p $pid > /dev/null 2>&1; then
                kill -9 $pid
            fi
            
            echo -e "${GREEN}✓ $service_name stopped${NC}"
        else
            echo -e "${YELLOW}$service_name is not running${NC}"
        fi
        rm "$pid_file"
    else
        echo -e "${YELLOW}No PID file found for $service_name${NC}"
    fi
}

# Stop services in reverse order
stop_service "frontend"
stop_service "api-gateway"
stop_service "media-service"
stop_service "product-service"
stop_service "user-service"
stop_service "service-registry"

echo ""
echo -e "${GREEN}✓ All services stopped${NC}"
echo ""
