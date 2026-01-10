#!/bin/bash
set -e

# Load configuration from environment or config file
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/config-loader.sh" ]; then
    source "$SCRIPT_DIR/config-loader.sh"
else
    # If config-loader not available, require direct environment variables
    : "${AWS_DEPLOY_HOST:?AWS_DEPLOY_HOST must be set}"
    : "${AWS_DEPLOY_USER:?AWS_DEPLOY_USER must be set}"
    : "${AWS_SSH_KEY:?AWS_SSH_KEY must be set}"
fi

# Get build number from argument with validation
BUILD_NUMBER=${1:?'BUILD_NUMBER is required. Usage: deploy.sh <BUILD_NUMBER>'}

# AWS Deployment Configuration (from environment - REQUIRED)
DEPLOY_HOST="${AWS_DEPLOY_USER}@${AWS_DEPLOY_HOST}"
DEPLOY_PATH="/home/${AWS_DEPLOY_USER}/buy-01-app"
SSH_KEY="${AWS_SSH_KEY}"

# Validate SSH key exists
if [ ! -f "$SSH_KEY" ]; then
    echo "❌ Error: SSH key not found at $SSH_KEY"
    echo "Please configure AWS_SSH_KEY or add credential in Jenkins"
    exit 1
fi

# Ensure proper SSH key permissions (required by SSH)
chmod 600 "$SSH_KEY" 2>/dev/null || true

AWS_PUBLIC_IP="${AWS_DEPLOY_HOST}"

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
echo -e "${YELLOW}[2/6] Preparing AWS deployment directory...${NC}"

# Ensure certs directory exists and is owned by the deploy user (fixes permission denied for Jenkins SCP)
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" "sudo mkdir -p $DEPLOY_PATH/certs && sudo chown -R ${AWS_DEPLOY_USER}:${AWS_DEPLOY_USER} $DEPLOY_PATH && sudo chmod -R 755 $DEPLOY_PATH"

# Backup current docker-compose.yml BEFORE transferring new one
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" bash -s <<BACKUP_FIRST
    cd "$DEPLOY_PATH"
    if [ -f docker-compose.yml ]; then
        cp docker-compose.yml docker-compose.yml.previous
        echo "✓ Backed up current docker-compose.yml"
    fi
BACKUP_FIRST

scp -i "$SSH_KEY" -o StrictHostKeyChecking=no .pipeline/docker-compose.yml "$DEPLOY_HOST:$DEPLOY_PATH/"

# Transfer .env file with secrets
if [ -f ".pipeline/.env.production" ]; then
    echo "Transferring production environment configuration..."
    echo "[DEBUG] Contents of local .env.production:"
    cat .pipeline/.env.production | sed 's/PASSWORD=.*/PASSWORD=***REDACTED***/'
    
    # Force remove old .env to ensure fresh file
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" "rm -f $DEPLOY_PATH/.env"
    
    scp -i "$SSH_KEY" -o StrictHostKeyChecking=no .pipeline/.env.production "$DEPLOY_HOST:$DEPLOY_PATH/.env"
    echo "✓ Environment configuration transferred"
    echo "[DEBUG] Verifying transferred .env on AWS:"
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" "cat $DEPLOY_PATH/.env | sed 's/PASSWORD=.*/PASSWORD=***REDACTED***/'"
fi

echo -e "${GREEN}✓ AWS directory prepared${NC}"
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
    # Verify image exists before attempting transfer
    if ! docker images "${IMAGE_PREFIX}-${service}:build-${BUILD_NUMBER}" | grep -q "build-${BUILD_NUMBER}"; then
        echo "ERROR: Image ${IMAGE_PREFIX}-${service}:build-${BUILD_NUMBER} not found!"
        docker images | grep "${IMAGE_PREFIX}-${service}"
        exit 1
    fi
    docker save ${IMAGE_PREFIX}-${service}:build-${BUILD_NUMBER} | ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" "docker load"
    echo "✓ ${service} transferred"
done

# Tag new images as latest on AWS (and backup old latest as previous)
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" bash <<TAGLATEST
    echo "Backing up current latest images as previous..."
    for service in service-registry api-gateway user-service product-service media-service frontend; do
        # First, backup current latest as previous (if it exists)
        if docker images "buy01-pipeline-\${service}:latest" | grep -q latest; then
            docker tag "buy01-pipeline-\${service}:latest" "buy01-pipeline-\${service}:previous" || true
            echo "  ✓ \${service}: backed up latest → previous"
        fi
        # Then tag new build as latest
        docker tag "buy01-pipeline-\${service}:build-${BUILD_NUMBER}" "buy01-pipeline-\${service}:latest"
    done
    echo "✓ New images tagged as latest, old latest backed up as previous"
TAGLATEST

echo -e "${GREEN}✓ All images transferred to AWS${NC}"
echo ""

# Transfer docker-compose.yml
echo -e "${YELLOW}[4/6] Verifying docker-compose.yml...${NC}"
echo -e "${GREEN}✓ docker-compose.yml already transferred and backed up${NC}"
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

# Wait for services to start with progressive health checks
echo "Waiting for services to initialize..."
echo "This may take up to 90 seconds for all services to be ready..."

HEALTH_CHECK_FAILED=0
MAX_RETRIES=18  # 18 retries x 5 seconds = 90 seconds max wait
RETRY_DELAY=5

# Function to check service with retries
check_service() {
    local service_name=$1
    local health_url=$2
    local retry_count=0
    
    echo -n "Checking $service_name..."
    while [ $retry_count -lt $MAX_RETRIES ]; do
        if curl -f -s "$health_url" > /dev/null 2>&1; then
            echo " ✓ healthy (after $((retry_count * RETRY_DELAY))s)"
            return 0
        fi
        retry_count=$((retry_count + 1))
        echo -n "."
        sleep $RETRY_DELAY
    done
    echo " ⚠ timeout after ${MAX_RETRIES} attempts"
    return 1
}

# Check Service Registry first (Eureka) - most critical
echo ""
echo "Waiting for Service Registry (Eureka) to be fully ready..."
if ! check_service "Service Registry" "http://localhost:8761"; then
    HEALTH_CHECK_FAILED=1
fi

# Wait extra time for Service Registry to register services
echo "Waiting for service discovery to stabilize..."
sleep 10

# Check API Gateway - depends on Service Registry being ready
if ! check_service "API Gateway" "http://localhost:8080/actuator/health"; then
    HEALTH_CHECK_FAILED=1
fi

# Check Frontend
echo -n "Checking Frontend..."
if curl -f -s "http://localhost:4200" > /dev/null 2>&1; then
    echo " ✓ healthy"
else
    echo " ⚠ frontend may still be starting"
fi

if [ $HEALTH_CHECK_FAILED -eq 1 ]; then
    echo ""
    echo "⚠ Some services are still initializing..."
    echo "Waiting 30 more seconds for services to stabilize..."
    sleep 30
    
    # Final retry
    if ! check_service "API Gateway (final check)" "http://localhost:8080/actuator/health"; then
        echo "❌ API Gateway still not responding after 120 seconds"
        exit 1
    fi
fi

echo ""
echo "✓ Services are healthy"

if [ $HEALTH_CHECK_FAILED -eq 1 ]; then
    echo ""
    echo "❌ Health checks failed! Deployment unsuccessful"
    echo "Checking container logs for troubleshooting..."
    docker-compose logs --tail=20 service-registry
    docker-compose logs --tail=20 api-gateway
    exit 1
fi

echo "✓ Initial health checks passed"

# CRITICAL: Verify ALL 9 containers are running (not just health checks)
echo ""
echo "Verifying all containers are running..."
RUNNING_COUNT=$(docker-compose ps | grep -c "Up" || echo 0)
EXPECTED_COUNT=9

if [ "$RUNNING_COUNT" -lt "$EXPECTED_COUNT" ]; then
    echo "❌ ERROR: Only $RUNNING_COUNT/$EXPECTED_COUNT containers running!"
    echo ""
    echo "Container status:"
    docker-compose ps
    echo ""
    echo "Checking for crashed services..."
    
    # Check each critical service
    for service in user-service product-service media-service api-gateway service-registry; do
        if ! docker-compose ps $service | grep -q "Up"; then
            echo ""
            echo "❌ $service is NOT running"
            echo "Last 30 lines of logs:"
            docker-compose logs --tail=30 $service
        fi
    done
    
    echo ""
    echo "❌ DEPLOYMENT FAILED: Not all services are running"
    exit 1
fi

echo "✓ All $RUNNING_COUNT/$EXPECTED_COUNT containers verified running"

# Wait extra time then verify containers STAYED running
echo ""
echo "Waiting 15 seconds then verifying containers stayed healthy..."
sleep 15

FINAL_RUNNING_COUNT=$(docker-compose ps | grep -c "Up" || echo 0)
if [ "$FINAL_RUNNING_COUNT" -lt "$EXPECTED_COUNT" ]; then
    echo "❌ ERROR: Container(s) crashed after startup! ($FINAL_RUNNING_COUNT/$EXPECTED_COUNT running)"
    echo ""
    
    # Show which services crashed
    for service in user-service product-service media-service api-gateway service-registry frontend mongodb kafka zookeeper; do
        if ! docker-compose ps $service | grep -q "Up"; then
            echo "❌ $service crashed! Last 50 lines:"
            docker-compose logs --tail=50 $service
            echo ""
        fi
    done
    
    exit 1
fi

echo "✓ All containers stayed healthy ($FINAL_RUNNING_COUNT/$EXPECTED_COUNT running)"

# Show running containers
echo ""
echo "Running containers:"
docker-compose ps
    
# Aggressive cleanup of old Docker resources to prevent disk from filling up
echo ""
echo "Cleaning up old Docker resources (keeping latest + previous + current build)..."
# Only remove dangling images (not tagged images) to preserve 'previous' backup
docker image prune -f
# Clean up build cache and unused volumes
docker builder prune -f --filter "until=1h"
docker volume prune -f
echo "✓ Cleanup completed (preserved latest and previous tags for rollback)"

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
