#!/bin/bash

################################################################################
# Pipeline Cleanup and Troubleshooting Script
# Helps diagnose and fix common issues with the pipeline infrastructure
#
# USAGE:
#   ./pipeline-tools.sh [COMMAND] [OPTIONS]
#
# COMMANDS:
#   diagnose            Run comprehensive system diagnostics
#   cleanup             Clean up all pipeline resources
#   logs [SERVICE]      Show logs for a service (jenkins, sonarqube, postgres)
#   disk-info           Show disk usage by Docker
#   docker-stats        Show real-time Docker stats
#   reset-jenkins       Remove Jenkins and rebuild image
#   help                Show this help message
#
# EXAMPLES:
#   ./pipeline-tools.sh diagnose
#   ./pipeline-tools.sh cleanup --force
#   ./pipeline-tools.sh logs jenkins
#   ./pipeline-tools.sh docker-stats
#
################################################################################

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

# Helper functions
log() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

error() {
    echo -e "${RED}❌ ERROR:${NC} $1"
}

success() {
    echo -e "${GREEN}✓ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠️  WARNING:${NC} $1"
}

info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

divider() {
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
}

section() {
    echo ""
    divider
    echo -e "${BLUE}  $1${NC}"
    divider
}

# Command: diagnose
diagnose() {
    section "PIPELINE DIAGNOSTICS"
    
    echo ""
    info "System Information"
    echo "  OS: $(uname -s)"
    echo "  Architecture: $(uname -m)"
    echo "  Kernel: $(uname -r)"
    
    echo ""
    info "Docker Status"
    if command -v docker &> /dev/null; then
        echo "  Docker: $(docker --version)"
        
        if docker ps &>/dev/null; then
            success "Docker daemon is accessible"
            echo "  Status: Running"
        else
            error "Cannot connect to Docker daemon"
            echo "  Status: Not accessible"
        fi
    else
        error "Docker is not installed"
    fi
    
    echo ""
    info "Docker Compose Status"
    if command -v docker-compose &> /dev/null; then
        echo "  Version: $(docker-compose --version)"
    elif command -v docker &> /dev/null && docker compose version &>/dev/null; then
        echo "  Version: $(docker compose version)"
        echo "  Type: Docker plugin"
    else
        error "Docker Compose is not installed"
    fi
    
    echo ""
    info "Pipeline Services"
    
    for SERVICE in jenkins-local sonarqube sonarqube-db; do
        if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${SERVICE}$"; then
            STATUS=$(docker ps --format '{{.State}}' -f "name=^${SERVICE}$" 2>/dev/null || echo "unknown")
            success "$SERVICE: Running ($STATUS)"
        elif docker ps -a --format '{{.Names}}' 2>/dev/null | grep -q "^${SERVICE}$"; then
            STATUS=$(docker ps -a --format '{{.State}}' -f "name=^${SERVICE}$" 2>/dev/null || echo "unknown")
            warning "$SERVICE: Stopped ($STATUS)"
        else
            warning "$SERVICE: Not created"
        fi
    done
    
    echo ""
    info "Required Tools"
    
    TOOLS=("git" "mvn" "node" "npm")
    for TOOL in "${TOOLS[@]}"; do
        if command -v "$TOOL" &> /dev/null; then
            VERSION=$("$TOOL" --version 2>/dev/null | head -1 || echo "version unknown")
            success "$TOOL: Installed ($VERSION)"
        else
            warning "$TOOL: Not installed"
        fi
    done
    
    echo ""
    info "Disk Usage"
    DOCKER_SIZE=$(du -sh ~/.docker 2>/dev/null | cut -f1 || echo "unknown")
    PROJECT_SIZE=$(du -sh "$PROJECT_ROOT" 2>/dev/null | cut -f1 || echo "unknown")
    echo "  Docker: $DOCKER_SIZE"
    echo "  Project: $PROJECT_SIZE"
    
    echo ""
    AVAILABLE=$(df / | awk 'NR==2 {print $4}' | awk '{printf "%.1f GB", $1/1024/1024}')
    info "Disk Available: $AVAILABLE"
    
    echo ""
}

# Command: cleanup
cleanup() {
    local FORCE=$1
    
    section "DOCKER CLEANUP"
    
    if [ "$FORCE" != "--force" ]; then
        echo -e "${YELLOW}This will remove unused Docker resources.${NC}"
        echo "Continue? (y/n)"
        read -r -t 10 RESPONSE
        if [ "$RESPONSE" != "y" ] && [ "$RESPONSE" != "Y" ]; then
            warning "Cleanup cancelled"
            return
        fi
    fi
    
    log "Stopping containers..."
    docker stop $(docker ps -q) 2>/dev/null || true
    
    log "Pruning unused resources..."
    RECLAIMED=$(docker system prune -f --volumes 2>/dev/null | grep -oP '\d+\.?\d*\s?[KMG]B' | tail -1 || echo "unknown amount")
    success "Cleanup complete. Reclaimed: $RECLAIMED"
    
    echo ""
}

# Command: logs
show_logs() {
    local SERVICE=$1
    
    if [ -z "$SERVICE" ]; then
        error "Please specify a service: jenkins, sonarqube, or postgres"
        return
    fi
    
    section "LOGS FOR $SERVICE"
    
    case "$SERVICE" in
        jenkins)
            docker logs --tail 100 -f jenkins-local
            ;;
        sonarqube)
            docker logs --tail 100 -f sonarqube
            ;;
        postgres|db)
            docker logs --tail 100 -f sonarqube-db
            ;;
        *)
            error "Unknown service: $SERVICE"
            ;;
    esac
}

# Command: disk-info
disk_info() {
    section "DOCKER DISK USAGE"
    
    docker system df
    
    echo ""
    info "Breakdown by component:"
    
    echo "  Images:"
    docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | head -10
    
    echo ""
    echo "  Containers:"
    docker ps -a --format "table {{.Names}}\t{{.Size}}" | head -10
    
    echo ""
    echo "  Volumes:"
    docker volume ls --format "table {{.Name}}\t{{.Mountpoint}}" || echo "  No volumes"
}

# Command: docker-stats
docker_stats() {
    section "DOCKER REAL-TIME STATISTICS"
    
    echo "Press Ctrl+C to exit"
    sleep 1
    
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"
    
    echo ""
    info "Continuous monitoring (Ctrl+C to exit):"
    docker stats --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.PIDs}}"
}

# Command: reset-jenkins
reset_jenkins() {
    section "RESETTING JENKINS"
    
    echo -e "${YELLOW}This will:${NC}"
    echo "  1. Stop the Jenkins container"
    echo "  2. Remove the container and volume"
    echo "  3. Rebuild the Jenkins image"
    echo "  4. Create a new container"
    echo ""
    echo "Continue? (y/n)"
    read -r -t 10 RESPONSE
    
    if [ "$RESPONSE" != "y" ] && [ "$RESPONSE" != "Y" ]; then
        warning "Reset cancelled"
        return
    fi
    
    log "Stopping Jenkins..."
    docker stop jenkins-local 2>/dev/null || true
    
    log "Removing Jenkins container and volume..."
    docker rm -f jenkins-local 2>/dev/null || true
    docker volume rm jenkins_home 2>/dev/null || true
    
    log "Rebuilding Jenkins image..."
    if docker build -t jenkins/jenkins:with-tools -f "${PROJECT_ROOT}/.pipeline/Dockerfile.jenkins" "${PROJECT_ROOT}/.pipeline/"; then
        success "Jenkins image rebuilt successfully"
    else
        error "Failed to rebuild Jenkins image"
        return
    fi
    
    echo ""
    success "Jenkins reset complete. Run ./boot-pipeline.sh to start it again."
}

# Command: help
show_help() {
    grep "^#" "$0" | grep -E "^# " | sed 's/^# //'
}

# Main command routing
COMMAND="${1:-help}"

case "$COMMAND" in
    diagnose) diagnose ;;
    cleanup) cleanup "$2" ;;
    logs) show_logs "$2" ;;
    disk-info) disk_info ;;
    docker-stats) docker_stats ;;
    reset-jenkins) reset_jenkins ;;
    help) show_help ;;
    *)
        error "Unknown command: $COMMAND"
        echo ""
        show_help
        exit 1
        ;;
esac
