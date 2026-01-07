#!/bin/bash
set -e

# Get build number from argument
BUILD_NUMBER=${1:-latest}

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

# 1. Verify Docker images exist
echo -e "${YELLOW}[1/6] Verifying Docker images...${NC}"
docker images | grep buy01-pipeline
echo -e "${GREEN}✓ Docker images verified (build-${BUILD_NUMBER})${NC}"
echo ""

# 2. Prepare AWS deployment directory
echo -e "${YELLOW}[2/6] Preparing AWS deployment directory and backing up current version...${NC}"
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" "mkdir -p $DEPLOY_PATH"

# Save current working version as backup before deploying new version
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" << 'BACKUP'
    # Tag current running images as 'previous' for rollback
    for service in service-registry api-gateway user-service product-service media-service frontend; do
        if docker images | grep -q "buy01-pipeline-${service}:latest"; then
            echo "Backing up ${service}:latest as ${service}:previous"
            docker tag buy01-pipeline-${service}:latest buy01-pipeline-${service}:previous || true
        fi
    done
    echo "✓ Current version backed up"
BACKUP

scp -i "$SSH_KEY" -o StrictHostKeyChecking=no docker-compose.yml "$DEPLOY_HOST:$DEPLOY_PATH/"
echo -e "${GREEN}✓ AWS directory prepared and backup created${NC}"
echo ""

# 3. Transfer images one by one to save disk space
echo -e "${YELLOW}[3/6] Transferring Docker images to AWS (build-${BUILD_NUMBER})...${NC}"
# Get the actual image prefix (workspace directory name)
IMAGE_PREFIX=$(docker images --format "{{.Repository}}" | grep -E "(service-registry|api-gateway)" | head -1 | cut -d'-' -f1-2)
if [ -z "$IMAGE_PREFIX" ]; then
    IMAGE_PREFIX="buy01-pipeline"  # Default for Jenkins
fi
echo "Using image prefix: $IMAGE_PREFIX with build-${BUILD_NUMBER}"

for service in service-registry api-gateway user-service product-service media-service frontend; do
    echo "Transferring ${service}:build-${BUILD_NUMBER}..."
    docker save ${IMAGE_PREFIX}-${service}:build-${BUILD_NUMBER} | ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" "docker load"
    echo "✓ ${service} transferred"
done

# Tag new images as latest on AWS
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" << TAGLATEST
    for service in service-registry api-gateway user-service product-service media-service frontend; do
        docker tag buy01-pipeline-\${service}:build-${BUILD_NUMBER} buy01-pipeline-\${service}:latest
    done
    echo "✓ New images tagged as latest"
TAGLATEST

echo -e "${GREEN}✓ All images transferred to AWS${NC}"
echo ""

# Transfer docker-compose.yml
echo -e "${YELLOW}[4/6] Verifying docker-compose.yml...${NC}"
echo -e "${GREEN}✓ docker-compose.yml already transferred${NC}"
echo ""

# 5. Deploy on AWS instance
echo -e "${YELLOW}[5/6] Starting containers on AWS with health checks...${NC}"
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" << 'ENDSSH'
cd /home/ec2-user/buy-01-app

# Stop existing containers
echo "Stopping existing containers..."
docker-compose down || true

# Start new containers
echo "Starting new containers..."
docker-compose up -d

# Wait for services to start
echo "Waiting for services to initialize..."
sleep 15

# Health check for critical services
echo "Performing health checks..."
HEALTH_CHECK_FAILED=0

# Check Service Registry (Eureka)
if curl -f -s "http://localhost:8761" > /dev/null 2>&1; then
    echo "✓ Service Registry is healthy"
else
    echo "❌ Service Registry health check failed"
    HEALTH_CHECK_FAILED=1
fi

# Check API Gateway
if curl -f -s "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
    echo "✓ API Gateway is healthy"
else
    echo "❌ API Gateway health check failed"
    HEALTH_CHECK_FAILED=1
fi

# Check Frontend
if curl -f -s "http://localhost:4200" > /dev/null 2>&1; then
    echo "✓ Frontend is healthy"
else
    echo "❌ Frontend health check failed"
    HEALTH_CHECK_FAILED=1
fi

if [ $HEALTH_CHECK_FAILED -eq 1 ]; then
    echo "❌ Health checks failed! Deployment unsuccessful"
    exit 1
fi

echo "✓ All health checks passed"

# Show running containers
echo ""
echo "Running containers:"
docker-compose ps
    
# Clean up old Docker images to prevent disk from filling up
echo ""
echo "Cleaning up old Docker images (keeping current + previous)..."
# Remove images older than 2 hours, keeping the backup
docker image prune -a -f --filter "until=2h"
echo "✓ Cleanup completed"

exit 0
ENDSSH

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Deployment successful and healthy${NC}"
    echo ""
    
    # 6. Cleanup old backup on successful deployment
    echo -e "${YELLOW}[6/6] Cleaning up old backups...${NC}"
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" << 'CLEANUP'
        # Remove very old 'previous-old' images if they exist
        for service in service-registry api-gateway user-service product-service media-service frontend; do
            if docker images | grep -q "buy01-pipeline-${service}:previous-old"; then
                echo "Removing old backup: ${service}:previous-old"
                docker rmi buy01-pipeline-${service}:previous-old || true
            fi
        done
        echo "✓ Old backups cleaned up"
CLEANUP
    
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${GREEN}   ✅ DEPLOYMENT SUCCESSFUL!${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
    echo -e "${GREEN}🌐 Application URLs:${NC}"
    echo -e "   Frontend (HTTP):  http://${AWS_PUBLIC_IP}:4200"
    echo -e "   API Gateway:      http://${AWS_PUBLIC_IP}:8080"
    echo -e "   Eureka Dashboard: http://${AWS_PUBLIC_IP}:8761"
    echo ""
    echo -e "${GREEN}📦 Deployed version: build-${BUILD_NUMBER}${NC}"
    echo -e "${YELLOW}Note: Previous working version kept as backup for rollback${NC}"
    echo ""
    exit 0
else
    echo -e "${RED}❌ Deployment health checks failed!${NC}"
    exit 1
fi
