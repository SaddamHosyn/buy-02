#!/bin/bash
set -e

# AWS Deployment Configuration
DEPLOY_HOST="ec2-user@51.21.198.139"
DEPLOY_PATH="/home/ec2-user/buy-01-app"

# Find SSH key - check multiple locations
if [ -f "/var/lib/jenkins/.ssh/aws-deploy-key.pem" ]; then
    SSH_KEY="/var/lib/jenkins/.ssh/aws-deploy-key.pem"
elif [ -f "$HOME/Downloads/lastreal.pem" ]; then
    SSH_KEY="$HOME/Downloads/lastreal.pem"
else
    echo "Error: SSH key not found!"
    exit 1
fi

AWS_PUBLIC_IP="51.21.198.139"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   AWS Deployment Started${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Check SSH key exists
if [ ! -f "$SSH_KEY" ]; then
    echo -e "${RED}❌ Error: SSH key not found at $SSH_KEY${NC}"
    exit 1
fi

# Ensure correct permissions on SSH key
chmod 600 "$SSH_KEY"

# 1. Build Docker images
echo -e "${YELLOW}[1/5] Building Docker images for AMD64 platform...${NC}"
docker-compose build --no-cache --build-arg BUILDPLATFORM=linux/amd64 || \
  DOCKER_DEFAULT_PLATFORM=linux/amd64 docker-compose build --no-cache
echo -e "${GREEN}✓ Docker images built${NC}"
echo ""

# 2. Prepare AWS directory
echo -e "${YELLOW}[2/5] Preparing AWS deployment directory...${NC}"
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" "mkdir -p $DEPLOY_PATH"
scp -i "$SSH_KEY" -o StrictHostKeyChecking=no docker-compose.yml "$DEPLOY_HOST:$DEPLOY_PATH/"
echo -e "${GREEN}✓ AWS directory prepared${NC}"
echo ""

# 3. Transfer images one by one to save disk space
echo -e "${YELLOW}[3/5] Transferring Docker images to AWS (one at a time to save disk)...${NC}"
# Get the actual image prefix (workspace directory name)
IMAGE_PREFIX=$(docker images --format "{{.Repository}}" | grep -E "(service-registry|api-gateway)" | head -1 | cut -d'-' -f1-2)
if [ -z "$IMAGE_PREFIX" ]; then
    IMAGE_PREFIX="buy01-pipeline"  # Default for Jenkins
fi
echo "Using image prefix: $IMAGE_PREFIX"

for service in service-registry api-gateway user-service product-service media-service frontend; do
    echo "Transferring ${service}..."
    docker save ${IMAGE_PREFIX}-${service}:latest | ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" "docker load"
    echo "✓ ${service} transferred"
done
echo -e "${GREEN}✓ All images transferred to AWS${NC}"
echo ""

# 4. Deploy on AWS instance
echo -e "${YELLOW}[4/5] Starting containers on AWS...${NC}"
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" << 'ENDSSH'
cd /home/ec2-user/buy-01-app

# Stop existing containers
echo "Stopping existing containers..."
docker-compose down || true

# Start new containers
echo "Starting containers..."
docker-compose up -d

# Show running containers
echo ""
echo "Running containers:"
docker-compose ps
    
# Clean up old Docker images and containers to free disk space
echo ""
echo "Cleaning up unused Docker resources..."
docker container prune -f
docker image prune -a -f --filter "until=24h"
echo "Cleanup completed"
sleep 10

# Check if services are responding
if curl -s "http://${AWS_PUBLIC_IP}:8761" > /dev/null; then
    echo -e "${GREEN}✓ Service Registry (Eureka) is responding${NC}"
else
    echo -e "${YELLOW}⚠ Service Registry not yet ready (may take a minute)${NC}"
fi

if curl -s "http://${AWS_PUBLIC_IP}:8080/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ API Gateway is responding${NC}"
else
    echo -e "${YELLOW}⚠ API Gateway not yet ready (may take a minute)${NC}"
fi

# Clean up local tar files
rm -f *.tar

echo ""
echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}   ✅ DEPLOYMENT SUCCESSFUL!${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "${GREEN}🌐 Application URLs:${NC}"
echo -e "   Frontend (HTTPS): https://${AWS_PUBLIC_IP}:4201"
echo -e "   Frontend (HTTP):  http://${AWS_PUBLIC_IP}:4200"
echo -e "   API Gateway:      http://${AWS_PUBLIC_IP}:8080"
echo -e "   Eureka Dashboard: http://${AWS_PUBLIC_IP}:8761"
echo ""
echo -e "${YELLOW}Note: Allow 1-2 minutes for all services to fully start${NC}"
echo ""
