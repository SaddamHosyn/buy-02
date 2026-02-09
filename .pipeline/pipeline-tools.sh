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
#   setup-job           Configure Jenkins credentials and pipeline job
#   help                Show this help message
#
# EXAMPLES:
#   ./pipeline-tools.sh diagnose
#   ./pipeline-tools.sh setup-job
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

# Load .env file from project root if it exists
if [ -f "${PROJECT_ROOT}/../.env" ]; then
    export $(grep -v '^#' "${PROJECT_ROOT}/../.env" | xargs)
fi

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

# Command: setup-job
setup_job() {
    section "JENKINS JOB AUTO-CONFIGURATION"
    
    local JENKINS_URL="http://localhost:8088"
    local CONTAINER_NAME="jenkins-local"
    local JOB_NAME="buy-02-pipeline"
    local REPO_URL=$(git config --get remote.origin.url || echo "https://github.com/SaddamHosyn/buy-02.git")
    
    log "Target URL: $JENKINS_URL"
    log "Repository: $REPO_URL"
    
    # Check if Jenkins is running
    if ! curl -s "$JENKINS_URL/login" > /dev/null; then
        error "Jenkins is not reachable at $JENKINS_URL. Please run boot-pipeline.sh first."
        return
    fi
    
    # Get Admin Password
    log "Retrieving Admin Password..."
    if ! PASSWORD=$(docker exec $CONTAINER_NAME cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null); then
        error "Could not read initialAdminPassword. This usually means Jenkins has already been set up or the volume contains old data."
        error "To fix this, please reset Jenkins to a clean state:"
        error "  ./.pipeline/pipeline-tools.sh reset-jenkins"
        return 1
    fi
    PASSWORD=$(echo "$PASSWORD" | tr -d '\r')
    success "Password retrieved"

    # Create Groovy Script using quoted heredoc to prevent shell expansion of Groovy variables
    cat > config_jenkins.groovy <<'EOF'
import jenkins.model.*
import org.jenkinsci.plugins.workflow.multibranch.*
import jenkins.branch.*
import jenkins.plugins.git.*
import com.cloudbees.hudson.plugins.folder.computed.DefaultOrphanedItemStrategy
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import org.jenkinsci.plugins.plaincredentials.impl.*
import hudson.util.Secret
import hudson.plugins.sonar.*
import hudson.plugins.sonar.model.*

def jenkins = Jenkins.instance

// --- CONFIGURING CREDENTIALS ---
println " Configuring Credentials..."
def systemCredentialsProvider = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0]
def store = systemCredentialsProvider.getStore()
def domain = Domain.global()

def credsToCreate = [
    [id: 'team-email', secret: 'dev-team@buy01.com', desc: 'Team Email'],
    [id: 'mongo-root-username', secret: 'admin', desc: 'MongoDB Root User'],
    [id: 'mongo-root-password', secret: 'admin123', desc: 'MongoDB Root Password'],
    [id: 'api-gateway-url', secret: 'http://api-gateway:8080', desc: 'API Gateway URL'],
    [id: 'github-token', secret: 'CHANGE_ME_GITHUB_TOKEN', desc: 'GitHub Token'],
    [id: 'sonarqube-token', secret: System.getenv('SONAR_AUTH_TOKEN') ?: 'CHANGE_ME_SONAR_TOKEN', desc: 'SonarQube Token']
]

credsToCreate.each { c ->
    def existing = new ArrayList(store.getCredentials(domain)).find { it.id == c.id }
    if (existing) {
        println "  Credential '${c.id}' already exists. Updating..."
        store.removeCredentials(domain, existing)
    }
    
    def cred = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        c.id,
        c.desc,
        Secret.fromString(c.secret)
    )
    store.addCredentials(domain, cred)
    println "  Credential '${c.id}' created/updated."
}

// --- CONFIGURING SONARQUBE SERVER ---
println " Configuring SonarQube Server..."
def sonarGlobalConf = jenkins.getDescriptor(SonarGlobalConfiguration.class)

// Attempt to define SonarQube installation
def instName = "SonarQube"
def serverUrl = "http://sonarqube:9000"
def credId = "sonarqube-token"
def triggers = new TriggersConfig()

try {
    // Constructor matching installed plugin:
    // (String name, String serverUrl, String credentialsId, String serverAuthenticationToken, String mojoVersion, TriggersConfig triggers, String additionalProperties)
    def sonarInst = new SonarInstallation(
        instName,
        serverUrl,
        credId,
        null, // serverAuthenticationToken
        null, // mojoVersion
        triggers,
        null  // additionalProperties
    )
    sonarGlobalConf.setInstallations(sonarInst)
    println "  SonarQube server '${instName}' configured."
} catch (Exception e) {
    println "  [ERROR] Could not configure SonarQube server: " + e.getMessage()
}
sonarGlobalConf.save()


// --- CONFIGURING 

// --- CONFIGURING JOB ---
def jobName = "JOB_NAME_PLACEHOLDER"
def repoUrl = "REPO_URL_PLACEHOLDER"
def scriptPath = ".pipeline/Jenkinsfile"
def project = jenkins.getItem(jobName)

if (project == null) {
    println "Creating new Multibranch Project: " + jobName
    project = jenkins.createProject(WorkflowMultiBranchProject.class, jobName)
} else {
    println "Updating existing project: " + jobName
}

def scmSource = new GitSCMSource(null, repoUrl, "", "*", "", false)
// scmSource.setCredentialsId("github-token") // Uncomment if using private repo
scmSource.setId("source-id-" + jobName)

def traits = new ArrayList()
traits.add(new jenkins.plugins.git.traits.BranchDiscoveryTrait())
scmSource.setTraits(traits)

def branchSource = new BranchSource(scmSource)
project.setSourcesList([branchSource])
project.setOrphanedItemStrategy(new DefaultOrphanedItemStrategy(true, "10", "10"))
project.getProjectFactory().setScriptPath(scriptPath)
project.save()
project.scheduleBuild2(0)

println "Job configured successfully!"
EOF

    # Replace placeholders with actual values (using temp file for macOS/Linux compatibility)
    sed "s/JOB_NAME_PLACEHOLDER/$JOB_NAME/" config_jenkins.groovy > config_jenkins.groovy.tmp && mv config_jenkins.groovy.tmp config_jenkins.groovy
    sed "s|REPO_URL_PLACEHOLDER|$REPO_URL|" config_jenkins.groovy > config_jenkins.groovy.tmp && mv config_jenkins.groovy.tmp config_jenkins.groovy

    # Execute Script via Jenkins REST API
    log "Sending configuration to Jenkins..."
    
    # CSRF Crumb handling
    CRUMB_ISSUER_URL="${JENKINS_URL}/crumbIssuer/api/json"
    COOKIE_JAR="/tmp/jenkins_cookies.txt"
    
    # Get Crumb and Cookie
    CRUMB_JSON=$(curl -s -u "admin:$PASSWORD" -c "$COOKIE_JAR" "$CRUMB_ISSUER_URL")
    CRUMB=$(echo "$CRUMB_JSON" | grep -o '"crumb":"[^"]*"' | awk -F':' '{print $2}' | tr -d '"')
    CRUMB_FIELD=$(echo "$CRUMB_JSON" | grep -o '"crumbRequestField":"[^"]*"' | awk -F':' '{print $2}' | tr -d '"')

    # Execute Script via Jenkins REST API
    log "Sending configuration to Jenkins..."
    
    if [ -n "$CRUMB" ]; then
        RESPONSE=$(curl -s -X POST -u "admin:$PASSWORD" -b "$COOKIE_JAR" -H "$CRUMB_FIELD: $CRUMB" --data-urlencode "script=$(cat config_jenkins.groovy)" "${JENKINS_URL}/scriptText")
    else
        RESPONSE=$(curl -s -X POST -u "admin:$PASSWORD" -b "$COOKIE_JAR" --data-urlencode "script=$(cat config_jenkins.groovy)" "${JENKINS_URL}/scriptText")
    fi

    rm config_jenkins.groovy

    if [[ "$RESPONSE" == *"Job configured successfully"* ]]; then
        success "Jenkins configured successfully!"
        success "  Job Link: ${JENKINS_URL}/job/${JOB_NAME}/"
    else
        error "Failed to configure Jenkins. Response:\n$RESPONSE"
    fi
}

# Command: setup-sonarqube
setup_sonarqube() {
    section "SONARQUBE AUTO-CONFIGURATION"
    
    local SONAR_URL="http://localhost:9000"
    local SONAR_PASS="${SONAR_ADMIN_PASSWORD:-admin123}"
    
    log "Target URL: $SONAR_URL"
    # Mask password in logs
    log "Using Admin Password: ${SONAR_PASS:0:3}*****"
    
    # Check if SonarQube is running
    if ! curl -s "$SONAR_URL/api/system/health" > /dev/null; then
        error "SonarQube is not reachable. Please run boot-pipeline.sh first."
        return
    fi
    
    # Attempt to change password (defaults to admin123 if env var not set)
    log "Attempting to set/verify admin password..."
    
    # Check if we can already login with the target password
    if curl -s -u "admin:$SONAR_PASS" "$SONAR_URL/api/authentication/validate" | grep -q "true"; then
        success "Authenticated successfully with configured password."
    else
        # Try to change it from default 'admin'
        log "Authentication failed. Attempting to change default password..."
        curl -s -u "admin:admin" -X POST "$SONAR_URL/api/users/change_password" -d "login=admin" -d "previousPassword=admin" -d "password=$SONAR_PASS" > /dev/null
        
        # Verify access again
        if curl -s -u "admin:$SONAR_PASS" "$SONAR_URL/api/authentication/validate" | grep -q "true"; then
             success "Default password changed successfully."
        else
             warning "Could not automatically set password."
             warning "1. Log in to http://localhost:9000"
             warning "2. Use admin/admin (or current password)"
             warning "3. Set the new password to: $SONAR_PASS"
             warning "4. Run this command again."
             return
        fi
    fi
    
    # Create Project
    log "Creating project 'buy-02'..."
    if curl -s -u "admin:$SONAR_PASS" -X POST "$SONAR_URL/api/projects/create" -d "name=buy-02" -d "project=buy-02" -d "visibility=public" | grep -q "project"; then
        success "Project 'buy-02' created"
    else
        log "Project likely already exists"
    fi
    
    # Generate Token
    log "Generating Jenkins Token..."
    TOKEN_RESPONSE=$(curl -s -u "admin:$SONAR_PASS" -X POST "$SONAR_URL/api/user_tokens/generate" -d "name=jenkins-token-$(date +%s)" -d "type=GLOBAL_ANALYSIS_TOKEN")
    
    TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$TOKEN" ]; then
        success "Token generated: $TOKEN"
        
        # Save token to .env for other scripts/functions to use
        if [ -f "${PROJECT_ROOT}/../.env" ]; then
             if grep -q "SONAR_AUTH_TOKEN=" "${PROJECT_ROOT}/../.env"; then
                 # Portable in-place edit for macOS/Linux
                 sed -i.bak "s|^SONAR_AUTH_TOKEN=.*|SONAR_AUTH_TOKEN=$TOKEN|" "${PROJECT_ROOT}/../.env" && rm "${PROJECT_ROOT}/../.env.bak"
             else
                 echo "SONAR_AUTH_TOKEN=$TOKEN" >> "${PROJECT_ROOT}/../.env"
             fi
             log "Token saved to .env file"
             export SONAR_AUTH_TOKEN=$TOKEN
        fi
        
        echo ""
        echo "IMPORTANT: Copy this token to update Jenkins credentials:"
        echo "$TOKEN"
    else
        warning "Could not generate token. Check logs."
    fi
}

# Command: scan
trigger_scan() {
    section "TRIGGERING SCAN"
    local JENKINS_URL="http://localhost:8088"
    local CONTAINER_NAME="jenkins-local"
    local JOB_NAME="buy-02-pipeline"
    
    # Get Password
    if ! PASSWORD=$(docker exec $CONTAINER_NAME cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null); then
        error "Could not read initialAdminPassword."
        return 1
    fi
    PASSWORD=$(echo "$PASSWORD" | tr -d '\r')
    
    # Get Crumb
    COOKIE_JAR="/tmp/jenkins_cookies.txt"
    CRUMB_ISSUER_URL="${JENKINS_URL}/crumbIssuer/api/json"
    CRUMB_JSON=$(curl -s -u "admin:$PASSWORD" -c "$COOKIE_JAR" "$CRUMB_ISSUER_URL")
    CRUMB=$(echo "$CRUMB_JSON" | grep -o '"crumb":"[^"]*"' | awk -F':' '{print $2}' | tr -d '"')
    CRUMB_FIELD=$(echo "$CRUMB_JSON" | grep -o '"crumbRequestField":"[^"]*"' | awk -F':' '{print $2}' | tr -d '"')
    
    log "Triggering scan for branch indexing..."
    
    if [ -n "$CRUMB" ]; then
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST -u "admin:$PASSWORD" -b "$COOKIE_JAR" -H "$CRUMB_FIELD: $CRUMB" "${JENKINS_URL}/job/${JOB_NAME}/build?delay=0")
    else
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST -u "admin:$PASSWORD" -b "$COOKIE_JAR" "${JENKINS_URL}/job/${JOB_NAME}/build?delay=0")
    fi
    
    if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ] || [ "$HTTP_CODE" == "302" ]; then
        success "Scan triggered successfully."
        log "Please check http://localhost:8088/job/buy-02-pipeline/computation/console for logs."
    else
        error "Failed to trigger scan. HTTP Code: $HTTP_CODE"
    fi
}

# Command: setup-certs
setup_certs() {
    section "CERTIFICATE SETUP"
    local SCRIPT="${PROJECT_ROOT}/setup_certs.sh"
    
    if [ ! -f "$SCRIPT" ]; then
        error "Certificate setup script not found at $SCRIPT"
    fi
    
    chmod +x "$SCRIPT"
    "$SCRIPT" || error "Failed to setup certificates"
}

# Command: ngrok
start_ngrok() {
    section "NGROK SETUP"
    
    if ! command -v ngrok &> /dev/null; then
        error "ngrok is not installed. Please install it first."
        return 1
    fi

    # Check if ngrok is already running
    if pgrep -x "ngrok" > /dev/null; then
        log "ngrok is already running."
    else
        log "Starting ngrok on port 8088..."
        ngrok http 8088 > /dev/null 2>&1 &
        sleep 3
    fi

    # Get Public URL
    local API_URL="http://localhost:4040/api/tunnels"
    if ! curl -s "$API_URL" > /dev/null; then
        error "Failed to retrieve ngrok URL. Is ngrok running?"
        return 1
    fi

    local PUBLIC_URL=$(curl -s "$API_URL" | jq -r '.tunnels[0].public_url')

    if [ -z "$PUBLIC_URL" ] || [ "$PUBLIC_URL" == "null" ]; then
        error "Could not find a public URL. Please check ngrok status."
        return 1
    fi
    
    success "ngrok is running!"
    echo -e "${GREEN}Public URL:${NC} $PUBLIC_URL"
    echo ""
    section "GITHUB WEBHOOK SETUP"
    echo "1. Go to your GitHub Repository -> Settings -> Webhooks"
    echo "2. Click 'Add webhook'"
    echo "3. Payload URL: ${PUBLIC_URL}/github-webhook/"
    echo "4. Content type: application/json"
    echo "5. Click 'Add webhook'"
    echo ""
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
    setup-job) setup_job ;;
    setup-sonarqube) setup_sonarqube ;;
    setup-certs) setup_certs ;;
    scan) trigger_scan ;;
    ngrok) start_ngrok ;;
    help) show_help ;;
    *)
        error "Unknown command: $COMMAND"
        echo ""
        show_help
        exit 1
        ;;
esac
