#!/bin/bash
set -e

# AWS Deployment Configuration
DEPLOY_HOST="ec2-user@51.21.198.139"
DEPLOY_PATH="/home/ec2-user/buy-01-app"

# Find SSH key
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

echo -e "${RED}============================================${NC}"
echo -e "${RED}   🔄 INITIATING ROLLBACK${NC}"
echo -e "${RED}============================================${NC}"
echo ""

# Ensure correct permissions on SSH key
chmod 600 "$SSH_KEY"

echo -e "${YELLOW}Rolling back to previous working version...${NC}"

ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$DEPLOY_HOST" << 'ROLLBACK'
cd /home/ec2-user/buy-01-app

# Stop failed deployment
echo "Stopping failed deployment..."
docker-compose down

# Check if previous backup exists
BACKUP_EXISTS=0
for service in service-registry api-gateway user-service product-service media-service frontend; do
    if docker images | grep -q "buy01-pipeline-${service}:previous"; then
        BACKUP_EXISTS=1
        break
    fi
done

if [ $BACKUP_EXISTS -eq 0 ]; then
    echo "❌ No previous backup found! Cannot rollback."
    exit 1
fi

# Restore previous working version
echo "Restoring previous working images..."
for service in service-registry api-gateway user-service product-service media-service frontend; do
    if docker images | grep -q "buy01-pipeline-${service}:previous"; then
        echo "Restoring ${service} from backup..."
        # Tag previous as latest to restore
        docker tag buy01-pipeline-${service}:previous buy01-pipeline-${service}:latest
    fi
done

# Start containers with restored images
echo "Starting containers with previous version..."
docker-compose up -d

# Wait for services
sleep 15

# Health check
echo "Verifying rollback health..."
HEALTH_OK=1

if ! curl -f -s "http://localhost:8761" > /dev/null 2>&1; then
    echo "⚠ Service Registry not responding"
    HEALTH_OK=0
fi

if ! curl -f -s "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
    echo "⚠ API Gateway not responding"
    HEALTH_OK=0
fi

if ! curl -f -s "http://localhost:4200" > /dev/null 2>&1; then
    echo "⚠ Frontend not responding"
    HEALTH_OK=0
fi

if [ $HEALTH_OK -eq 1 ]; then
    echo "✓ Rollback successful - previous version restored and healthy"
    exit 0
else
    echo "⚠ Rollback completed but some services may need more time to start"
    exit 0
fi
ROLLBACK

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${GREEN}   ✅ ROLLBACK SUCCESSFUL!${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
    echo -e "${GREEN}🌐 Application restored at:${NC}"
    echo -e "   Frontend:         http://${AWS_PUBLIC_IP}:4200"
    echo -e "   API Gateway:      http://${AWS_PUBLIC_IP}:8080"
    echo -e "   Eureka Dashboard: http://${AWS_PUBLIC_IP}:8761"
    echo ""
    echo -e "${YELLOW}⚠ Previous working version has been restored${NC}"
    echo -e "${YELLOW}⚠ Please investigate the deployment failure${NC}"
    echo ""
    exit 0
else
    echo -e "${RED}❌ Rollback encountered issues${NC}"
    exit 1
fi
