#!/bin/bash

################################################################################
# Environment Validation Script - SIMPLIFIED AND FIXED
# Validates critical tools for CI/CD pipeline
#
# EXIT CODES:
#   0 - All critical validations passed (warnings OK)
#   1 - Critical tool missing (build will fail)
#
################################################################################

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

PASSED=0
FAILED=0

echo -e "${BLUE}════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   Environment Validation${NC}"
echo -e "${BLUE}════════════════════════════════════════════════${NC}"
echo ""

# CRITICAL TOOLS - Pipeline will fail if these are missing
echo -e "${CYAN}CRITICAL BUILD TOOLS:${NC}"

check_critical() {
    local name="$1"
    local cmd="$2"
    
    echo -n "Checking $name... "
    if command -v "$cmd" &>/dev/null; then
        local ver=$("$cmd" --version 2>/dev/null | head -1 || echo "installed")
        echo -e "${GREEN}✓${NC} ($ver)"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}✗ MISSING${NC}"
        FAILED=$((FAILED + 1))
    fi
}

check_critical "Maven" "mvn"
check_critical "Node.js" "node"
check_critical "npm" "npm"
check_critical "Git" "git"
check_critical "Docker CLI" "docker"

echo ""
echo -e "${CYAN}SUPPORTING TOOLS:${NC}"

# Docker socket - just check it exists, don't fail if not accessible
echo -n "Checking Docker socket... "
if [ -S /var/run/docker.sock ]; then
    echo -e "${GREEN}✓${NC} (mounted)"
else
    echo -e "${YELLOW}⚠${NC} (not found - deployment may fail)"
fi

# Chromium - optional but check anyway
echo -n "Checking Chromium... "
if command -v chromium &>/dev/null; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${YELLOW}⚠${NC} (optional - frontend tests may fail)"
fi

echo ""
echo -e "${BLUE}════════════════════════════════════════════════${NC}"

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ Environment validation PASSED${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════${NC}"
    exit 0
else
    echo -e "${RED}✗ Environment validation FAILED${NC}"
    echo -e "${RED}  $FAILED critical tool(s) missing${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════${NC}"
    exit 1
fi

echo ""
