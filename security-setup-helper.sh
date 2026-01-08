#!/bin/bash

# Jenkins Security Setup Helper Script
# This script helps you secure your Jenkins installation

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}   Jenkins Security Configuration Helper${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""

# Check if running as root
if [ "$EUID" -eq 0 ]; then 
    echo -e "${RED}⚠️  Do not run this script as root!${NC}"
    echo "Run as jenkins user or regular user with sudo access"
    exit 1
fi

echo -e "${YELLOW}This script will help you:${NC}"
echo "1. Configure SSH known_hosts for secure connections"
echo "2. Generate secure passwords"
echo "3. Verify Jenkins credentials setup"
echo ""

# Function to add AWS host to known_hosts
setup_known_hosts() {
    echo -e "${YELLOW}Setting up SSH known_hosts...${NC}"
    read -p "Enter AWS host IP (e.g., 13.61.234.232): " AWS_IP
    
    if [ -z "$AWS_IP" ]; then
        echo -e "${RED}❌ No IP provided${NC}"
        return
    fi
    
    # Add to known_hosts
    ssh-keyscan -H "$AWS_IP" >> ~/.ssh/known_hosts 2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ SSH host key added to known_hosts${NC}"
        echo "Verify with: ssh-keygen -H -F $AWS_IP"
    else
        echo -e "${RED}❌ Failed to add host key${NC}"
    fi
}

# Function to generate secure passwords
generate_passwords() {
    echo -e "${YELLOW}Generating secure passwords...${NC}"
    echo ""
    echo "MongoDB Root Password:"
    openssl rand -base64 32
    echo ""
    echo "Jenkins Admin Password:"
    openssl rand -base64 24
    echo ""
    echo -e "${GREEN}✅ Save these passwords securely!${NC}"
    echo "Add them to Jenkins Credentials Store"
}

# Function to check Jenkins credentials
check_credentials() {
    echo -e "${YELLOW}Checking Jenkins credentials configuration...${NC}"
    
    JENKINS_HOME="${JENKINS_HOME:-/var/lib/jenkins}"
    
    if [ ! -d "$JENKINS_HOME" ]; then
        echo -e "${RED}❌ Jenkins home not found at $JENKINS_HOME${NC}"
        return
    fi
    
    CREDS_FILE="$JENKINS_HOME/credentials.xml"
    
    if [ -f "$CREDS_FILE" ]; then
        echo -e "${GREEN}✅ Jenkins credentials file exists${NC}"
        echo "Location: $CREDS_FILE"
        
        # Count credentials (rough estimate)
        COUNT=$(grep -c "<entry>" "$CREDS_FILE" 2>/dev/null || echo "0")
        echo "Estimated credential entries: $COUNT"
    else
        echo -e "${YELLOW}⚠️  No credentials file found${NC}"
        echo "You may need to configure Jenkins credentials first"
    fi
}

# Function to verify .gitignore
check_gitignore() {
    echo -e "${YELLOW}Checking .gitignore for sensitive files...${NC}"
    
    if [ -f ".gitignore" ]; then
        if grep -q "\.env" .gitignore && grep -q "\.pem" .gitignore; then
            echo -e "${GREEN}✅ .gitignore properly configured${NC}"
        else
            echo -e "${YELLOW}⚠️  .gitignore may be missing entries${NC}"
            echo "Ensure these patterns are in .gitignore:"
            echo "  .env"
            echo "  .env.*"
            echo "  *.pem"
            echo "  *.key"
        fi
    else
        echo -e "${RED}❌ No .gitignore found!${NC}"
    fi
}

# Main menu
while true; do
    echo ""
    echo -e "${GREEN}Select an option:${NC}"
    echo "1) Setup SSH known_hosts for AWS"
    echo "2) Generate secure passwords"
    echo "3) Check Jenkins credentials"
    echo "4) Verify .gitignore"
    echo "5) All checks"
    echo "6) Exit"
    echo ""
    read -p "Choice [1-6]: " choice
    
    case $choice in
        1) setup_known_hosts ;;
        2) generate_passwords ;;
        3) check_credentials ;;
        4) check_gitignore ;;
        5)
            setup_known_hosts
            echo ""
            generate_passwords
            echo ""
            check_credentials
            echo ""
            check_gitignore
            ;;
        6) 
            echo -e "${GREEN}Done! Review JENKINS_SECURITY_SETUP.md for complete setup${NC}"
            exit 0
            ;;
        *) 
            echo -e "${RED}Invalid choice${NC}"
            ;;
    esac
done
