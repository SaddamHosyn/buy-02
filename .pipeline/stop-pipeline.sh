#!/bin/bash

#############################################################
# Pipeline Infrastructure Stop Script
# Stops Jenkins, SonarQube, and ngrok
#############################################################

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   Pipeline Infrastructure Shutdown${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Stop ngrok
echo -e "${YELLOW}[1/3] Stopping ngrok...${NC}"
pkill -f "ngrok.*8088" > /dev/null 2>&1 && echo -e "${GREEN}✓ ngrok stopped${NC}" || echo -e "${YELLOW}  (ngrok was not running)${NC}"
echo ""

# Stop Jenkins
echo -e "${YELLOW}[2/3] Stopping Jenkins...${NC}"
if docker ps --format '{{.Names}}' | grep -q '^jenkins-local$'; then
    docker stop jenkins-local > /dev/null 2>&1
    echo -e "${GREEN}✓ Jenkins stopped${NC}"
else
    echo -e "${YELLOW}  (Jenkins was not running)${NC}"
fi
echo ""

# Stop SonarQube and its database
echo -e "${YELLOW}[3/3] Stopping SonarQube and PostgreSQL...${NC}"
cd "$(dirname "$0")/.pipeline"
docker compose stop sonarqube sonarqube-db 2>/dev/null || true
echo -e "${GREEN}✓ SonarQube and PostgreSQL stopped${NC}"
echo ""

echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}✓ Pipeline Infrastructure Stopped${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "${YELLOW}To restart: ./boot-pipeline.sh${NC}"
echo ""
