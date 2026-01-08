#!/bin/bash

# Environment Validation Script
# Checks if all required tools and dependencies are available

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   Environment Validation${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

VALIDATION_FAILED=0

# Check Maven
echo -n "Checking Maven... "
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -v | head -1)
    echo -e "${GREEN}✓${NC} ($MVN_VERSION)"
else
    echo -e "${RED}✗ NOT FOUND${NC}"
    echo "  Please install Maven: https://maven.apache.org/install.html"
    VALIDATION_FAILED=1
fi

# Check Node.js
echo -n "Checking Node.js... "
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v)
    echo -e "${GREEN}✓${NC} (v$NODE_VERSION)"
else
    echo -e "${RED}✗ NOT FOUND${NC}"
    echo "  Please install Node.js: https://nodejs.org/"
    VALIDATION_FAILED=1
fi

# Check npm
echo -n "Checking npm... "
if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm -v)
    echo -e "${GREEN}✓${NC} (v$NPM_VERSION)"
else
    echo -e "${RED}✗ NOT FOUND${NC}"
    echo "  Please install npm: https://www.npmjs.com/get-npm"
    VALIDATION_FAILED=1
fi

# Check Docker
echo -n "Checking Docker... "
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker -v)
    echo -e "${GREEN}✓${NC} ($DOCKER_VERSION)"
else
    echo -e "${RED}✗ NOT FOUND${NC}"
    echo "  Please install Docker: https://docs.docker.com/get-docker/"
    VALIDATION_FAILED=1
fi

# Check Docker daemon is running
echo -n "Checking Docker daemon... "
if docker ps > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} (running)"
else
    echo -e "${RED}✗ NOT RUNNING${NC}"
    echo "  Please start Docker daemon"
    VALIDATION_FAILED=1
fi

# Check docker-compose
echo -n "Checking docker-compose... "
if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose -v)
    echo -e "${GREEN}✓${NC} ($COMPOSE_VERSION)"
else
    echo -e "${RED}✗ NOT FOUND${NC}"
    echo "  Please install docker-compose: https://docs.docker.com/compose/install/"
    VALIDATION_FAILED=1
fi

# Check Git
echo -n "Checking Git... "
if command -v git &> /dev/null; then
    GIT_VERSION=$(git --version)
    echo -e "${GREEN}✓${NC} ($GIT_VERSION)"
else
    echo -e "${RED}✗ NOT FOUND${NC}"
    echo "  Please install Git: https://git-scm.com/"
    VALIDATION_FAILED=1
fi

# Check Chrome/Chromium for frontend tests
echo -n "Checking Chrome (for frontend tests)... "
CHROME_FOUND=0
if [ -n "${CHROME_BIN:-}" ] && [ -x "$CHROME_BIN" ]; then
    echo -e "${GREEN}✓${NC} (CHROME_BIN set)"
    CHROME_FOUND=1
elif command -v google-chrome &> /dev/null; then
    echo -e "${GREEN}✓${NC} (google-chrome)"
    CHROME_FOUND=1
elif command -v chromium-browser &> /dev/null; then
    echo -e "${GREEN}✓${NC} (chromium-browser)"
    CHROME_FOUND=1
elif command -v chromium &> /dev/null; then
    echo -e "${GREEN}✓${NC} (chromium)"
    CHROME_FOUND=1
else
    echo -e "${YELLOW}⚠${NC} (optional, frontend tests may fail)"
fi

echo ""

if [ $VALIDATION_FAILED -eq 1 ]; then
    echo -e "${RED}============================================${NC}"
    echo -e "${RED}❌ Environment validation FAILED${NC}"
    echo -e "${RED}============================================${NC}"
    echo ""
    echo "Please install missing dependencies and try again."
    exit 1
else
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}✓ Environment validation passed${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""
    exit 0
fi
