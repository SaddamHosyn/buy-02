#!/bin/bash

#############################################################
# Pipeline Infrastructure Boot Script
# Starts Jenkins, SonarQube, PostgreSQL, and ngrok
# 
# USAGE:
#   ./boot-pipeline.sh [OPTIONS]
#
# OPTIONS:
#   --help              Show this help message
#   --restart           Restart existing containers (preserve all data)
#   --cleanup           Remove all containers and volumes first
#   --no-ngrok          Skip ngrok tunnel setup (local only)
#   --rebuild-jenkins   Force rebuild of Jenkins image
#
# PREREQUISITES:
#   - Docker and Docker Compose installed and running
#   - ngrok installed (for webhook support)
#   - Ports 8088, 9000, 5432, 4040 available
#   - At least 2GB free disk space
#
#############################################################

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_FILE="${PROJECT_ROOT}/boot.log"

# Parse command line arguments
CLEANUP=false
RESTART_ONLY=false
SKIP_NGROK=false
REBUILD_JENKINS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --restart) RESTART_ONLY=true; shift ;;
        --cleanup) CLEANUP=true; shift ;;
        --no-ngrok) SKIP_NGROK=true; shift ;;
        --rebuild-jenkins) REBUILD_JENKINS=true; shift ;;
        --help) show_help; exit 0 ;;
        *) echo "Unknown option: $1"; show_help; exit 1 ;;
    esac
done

# Helper functions
show_help() {
    grep "^#" "$0" | grep -E "^# " | sed 's/^# //'
}

log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}❌ ERROR:${NC} $1" | tee -a "$LOG_FILE"
    exit 1
}

success() {
    echo -e "${GREEN}✓ $1${NC}" | tee -a "$LOG_FILE"
}

warning() {
    echo -e "${YELLOW}⚠️  WARNING:${NC} $1" | tee -a "$LOG_FILE"
}

section() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════${NC}"
}

# Initialize log file
: > "$LOG_FILE"
log "Pipeline Infrastructure Boot started"
log "Options: CLEANUP=$CLEANUP, SKIP_NGROK=$SKIP_NGROK, REBUILD_JENKINS=$REBUILD_JENKINS"

section "PREREQUISITE CHECKS"

# Check Docker is running
log "Checking Docker daemon..."
if ! docker info > /dev/null 2>&1; then
    error "Docker is not running! Please start Docker Desktop or docker daemon"
fi
success "Docker daemon is running"

# Check Docker Compose
log "Checking Docker Compose..."
if ! command -v docker-compose &> /dev/null && ! docker compose --help &> /dev/null; then
    error "Docker Compose is not installed. Please install Docker Compose."
fi
success "Docker Compose is available"

# Check disk space
log "Checking disk space..."
AVAILABLE_SPACE=$(df "$PROJECT_ROOT" | awk 'NR==2 {print $4}')
if [ "$AVAILABLE_SPACE" -lt 2097152 ]; then # 2GB in KB
    warning "Less than 2GB free disk space. Recommended: 5GB+ for development"
    read -p "Continue anyway? (y/n) " -n 1 -r; echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log "Cancelled by user"
        exit 0
    fi
fi
success "Sufficient disk space available"

# Cleanup if requested
if [ "$CLEANUP" = true ]; then
    section "CLEANUP"
    log "Cleaning up Docker resources..."
    docker stop jenkins-local sonarqube sonarqube-db buy01-sonarqube buy01-sonarqube-db 2>/dev/null || true
    docker rm jenkins-local sonarqube sonarqube-db buy01-sonarqube buy01-sonarqube-db 2>/dev/null || true
    docker volume rm jenkins_home pipeline_sonarqube_data pipeline_sonarqube_db pipeline_sonarqube_extensions pipeline_sonarqube_logs 2>/dev/null || true
    success "Cleanup completed"
fi

# Quick restart mode (stop and restart existing containers only)
if [ "$RESTART_ONLY" = true ]; then
    section "RESTART MODE (Preserving all data)"
    log "Stopping services gracefully..."
    docker stop jenkins-local sonarqube sonarqube-db 2>/dev/null || true
    sleep 2
    log "Restarting services..."
    docker start sonarqube-db sonarqube jenkins-local 2>/dev/null || warning "Some containers may not exist"
    success "Services restarted"
    SKIP_REBUILD=true
    REBUILD_JENKINS=false
fi

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   Pipeline Infrastructure Boot${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Check Docker is running
log "Checking Docker daemon..."
if ! docker info > /dev/null 2>&1; then
    error "Docker is not running!"
fi
success "Docker is running"
echo ""

# Skip full startup in restart mode - containers are already started above
if [ "$RESTART_ONLY" = true ]; then
    section "WAITING FOR SERVICES TO BE READY"
    log "Services already restarted, waiting for initialization..."
else
    # Start SonarQube + Database
    section "STARTING SONARQUBE & DATABASE"

    cd "$PROJECT_ROOT" || error "Cannot change to pipeline directory"

    log "Starting SonarQube infrastructure..."
    docker compose up -d sonarqube-db sonarqube 2>&1 | tee -a "$LOG_FILE" || error "Failed to start SonarQube"

    success "SonarQube containers started"
    log "Waiting for SonarQube to initialize (this takes ~30 seconds)..."
fi

# Wait for SonarQube to be ready
if [ "$RESTART_ONLY" != true ]; then
    MAX_ATTEMPTS=40
    ATTEMPT=1
    SONARQUBE_READY=false

    while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
        if curl -s http://localhost:9000/api/system/health 2>/dev/null | grep -q '"health":"UP"'; then
            SONARQUBE_READY=true
            break
        fi
        
        if [ $((ATTEMPT % 10)) -eq 0 ]; then
            log "  Attempt $ATTEMPT/$MAX_ATTEMPTS..."
        fi
        
        sleep 1
        ATTEMPT=$((ATTEMPT + 1))
    done

    if [ "$SONARQUBE_READY" = true ]; then
        success "SonarQube is ready"
        success "  URL: http://localhost:9000"
        success "  Credentials: admin/admin"
    else
        warning "SonarQube not responding after $MAX_ATTEMPTS seconds"
        warning "  Check status: docker logs sonarqube"
        warning "  Container may still be initializing..."
    fi
fi

echo ""

# Start Jenkins
section "STARTING JENKINS"

# Build custom Jenkins image if needed
JENKINS_IMAGE="jenkins/jenkins:with-tools"
log "Checking Jenkins image..."

if [ "$REBUILD_JENKINS" = true ] || ! docker image inspect "$JENKINS_IMAGE" > /dev/null 2>&1; then
    if [ "$REBUILD_JENKINS" = true ]; then
        log "Rebuilding Jenkins image (forced)..."
        docker rmi "$JENKINS_IMAGE" 2>/dev/null || true
    else
        log "Jenkins image not found, building..."
    fi
    
    if docker build -f "$PROJECT_ROOT/Dockerfile.jenkins" -t "$JENKINS_IMAGE" "$PROJECT_ROOT" >> "$LOG_FILE" 2>&1; then
        success "Jenkins image built successfully"
    else
        error "Failed to build Jenkins image. Check log: $LOG_FILE"
    fi
else
    success "Jenkins image exists and is up-to-date"
fi

# Verify Jenkins image has required tools
log "Verifying Jenkins image tools..."
MISSING_TOOLS=()

for tool in "chromium" "mvn" "node" "npm" "docker"; do
    if ! docker run --rm "$JENKINS_IMAGE" which "$tool" > /dev/null 2>&1; then
        MISSING_TOOLS+=("$tool")
    fi
done

if [ ${#MISSING_TOOLS[@]} -gt 0 ]; then
    warning "Missing tools in Jenkins image: ${MISSING_TOOLS[*]}"
    warning "Image may not have been built correctly. Rebuilding..."
    docker rmi "$JENKINS_IMAGE" 2>/dev/null || true
    if ! docker build -f "$PROJECT_ROOT/Dockerfile.jenkins" -t "$JENKINS_IMAGE" "$PROJECT_ROOT" >> "$LOG_FILE" 2>&1; then
        error "Failed to rebuild Jenkins image"
    fi
    success "Jenkins image rebuilt"
else
    success "All required tools present in Jenkins image"
fi

# Fix Docker socket permissions for Jenkins access
log "Setting up Docker socket permissions..."
if [ -S /var/run/docker.sock ]; then
    # Make docker socket world-accessible so Jenkins inside container can use it
    # This is necessary because the container's jenkins user GID won't match host's docker GID
    sudo chmod 666 /var/run/docker.sock 2>/dev/null || {
        echo "⚠️  Cannot chmod docker socket (may need manual intervention)"
        echo "    Try: sudo chmod 666 /var/run/docker.sock"
    }
    
    success "Docker socket permissions configured"
else
    warning "Docker socket not found at /var/run/docker.sock"
fi

# Check if jenkins-local container exists
if docker ps -a --format '{{.Names}}' 2>/dev/null | grep -q '^jenkins-local$'; then
    log "Jenkins container found, starting..."
    if docker start jenkins-local >> "$LOG_FILE" 2>&1; then
        success "Jenkins container started"
    else
        warning "Failed to start existing container, will recreate..."
        docker rm -f jenkins-local 2>/dev/null || true
        
        docker run -d \
            --name jenkins-local \
            -p 8088:8080 \
            -p 50000:50000 \
            -e DOCKER_HOST=unix:///var/run/docker.sock \
            -v jenkins_home:/var/jenkins_home \
            -v /var/run/docker.sock:/var/run/docker.sock:rw \
            "$JENKINS_IMAGE" >> "$LOG_FILE" 2>&1 || error "Failed to create Jenkins container"
        success "Jenkins container created"
    fi
else
    log "Jenkins container not found, creating..."
    docker run -d \
        --name jenkins-local \
        -p 8088:8080 \
        -p 50000:50000 \
        -e DOCKER_HOST=unix:///var/run/docker.sock \
        -v jenkins_home:/var/jenkins_home \
        -v /var/run/docker.sock:/var/run/docker.sock:rw \
        "$JENKINS_IMAGE" >> "$LOG_FILE" 2>&1 || error "Failed to create Jenkins container"
    success "Jenkins container created"
fi

log "Waiting for Jenkins to initialize..."

# Wait for Jenkins to be ready
MAX_ATTEMPTS=60
ATTEMPT=1
JENKINS_READY=false

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if curl -s http://localhost:8088/api/json > /dev/null 2>&1; then
        JENKINS_READY=true
        break
    fi
    
    if [ $((ATTEMPT % 10)) -eq 0 ]; then
        log "  Attempt $ATTEMPT/$MAX_ATTEMPTS..."
    fi
    
    sleep 1
    ATTEMPT=$((ATTEMPT + 1))
done

if [ "$JENKINS_READY" = true ]; then
    success "Jenkins is ready"
    success "  URL: http://localhost:8088"
    
    # Verify Jenkins can access Docker socket
    log "Verifying Docker socket access from Jenkins..."
    if docker exec jenkins-local docker ps > /dev/null 2>&1; then
        success "Jenkins Docker socket access verified ✓"
    else
        warning "Jenkins may have Docker socket access issues"
        warning "  If deployment fails with 'permission denied', run:"
        warning "  sudo chmod g+rw /var/run/docker.sock"
        warning "  Then restart: ./stop_pipeline.sh && ./boot-pipeline.sh"
    fi
else
    warning "Jenkins not responding after $MAX_ATTEMPTS seconds"
    warning "  Check status: docker logs jenkins-local"
    warning "  Container may still be initializing..."
fi

echo ""

# Start ngrok tunnel
if [ "$SKIP_NGROK" = true ]; then
    section "SKIPPING NGROK (LOCAL MODE)"
    warning "GitHub webhooks disabled. Use --no-ngrok=false for webhook support."
else
    section "STARTING NGROK TUNNEL"
    
    log "Checking ngrok installation..."
    if ! command -v ngrok &> /dev/null; then
        error "ngrok is not installed. Install from: https://ngrok.com/download"
    fi
    success "ngrok is installed"
    
    log "Setting up GitHub webhook tunnel..."
    
    # Kill any existing ngrok processes
    pkill -f "ngrok.*http.*8088" > /dev/null 2>&1 || true
    sleep 2
    
    # Start ngrok in background
    ngrok http 8088 > /tmp/ngrok.log 2>&1 &
    NGROK_PID=$!
    echo "$NGROK_PID" > /tmp/ngrok.pid
    success "ngrok process started (PID: $NGROK_PID)"
    
    log "Waiting for ngrok tunnel to establish..."
    sleep 5
    
    # Extract ngrok URL with retry
    MAX_ATTEMPTS=10
    ATTEMPT=1
    NGROK_URL=""
    
    while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
        NGROK_URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | grep -o '"public_url":"[^"]*"' | head -1 | cut -d'"' -f4)
        
        if [ -n "$NGROK_URL" ]; then
            break
        fi
        
        log "  Attempt $ATTEMPT/$MAX_ATTEMPTS..."
        sleep 1
        ATTEMPT=$((ATTEMPT + 1))
    done
    
    if [ -z "$NGROK_URL" ]; then
        error "Failed to get ngrok tunnel URL. Check: cat /tmp/ngrok.log"
    fi
    
    success "ngrok tunnel established"
    success "  Public URL: $NGROK_URL"
    echo -e "${CYAN}  GitHub webhook URL: $NGROK_URL/github-webhook/${NC}"
fi

cd "$PROJECT_ROOT" || error "Cannot change to project root"
echo ""
echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}✓ Pipeline Infrastructure is Ready!${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "${BLUE}Quick Links:${NC}"
echo -e "  Jenkins:    ${GREEN}http://localhost:8088${NC}"
echo -e "  SonarQube:  ${GREEN}http://localhost:9000${NC}"
echo -e "  ngrok:      ${GREEN}http://localhost:4040${NC}"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo -e "  1. Push to any branch to trigger builds"
echo -e "  2. Monitor Jenkins console: http://localhost:8088/queue/"

echo -e "  3. View SonarQube analysis: http://localhost:9000/projects"
echo ""
echo -e "${YELLOW}To stop everything: ./stop_pipeline.sh${NC}"
echo ""
