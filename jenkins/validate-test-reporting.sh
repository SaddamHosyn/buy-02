#!/bin/bash

# Test Reporting Implementation Validation Script
# This script verifies that all test reporting components are correctly configured

set -e

echo "======================================"
echo "Test Reporting Implementation Checker"
echo "======================================"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ISSUES=0

# Check 1: Jenkinsfile has enhanced post section
echo "📋 Checking Jenkinsfile post section..."
if grep -q "publishHTML" Jenkinsfile && grep -q "junit(" Jenkinsfile && grep -q "archiveArtifacts" Jenkinsfile; then
    echo -e "${GREEN}✅ Jenkinsfile post section is enhanced${NC}"
else
    echo -e "${RED}❌ Jenkinsfile missing enhanced post section${NC}"
    ISSUES=$((ISSUES + 1))
fi

# Check 2: JaCoCo plugin in pom.xml
echo ""
echo "📦 Checking JaCoCo plugin in pom.xml..."
if grep -q "jacoco-maven-plugin" pom.xml && grep -q "prepare-agent" pom.xml; then
    echo -e "${GREEN}✅ JaCoCo plugin configured in pom.xml${NC}"
else
    echo -e "${RED}❌ JaCoCo plugin not found in pom.xml${NC}"
    ISSUES=$((ISSUES + 1))
fi

# Check 3: Maven Surefire plugin configured
echo ""
echo "🧪 Checking Maven Surefire plugin..."
if grep -q "maven-surefire-plugin" pom.xml && grep -q "surefireArgLine" pom.xml; then
    echo -e "${GREEN}✅ Maven Surefire plugin configured with JaCoCo${NC}"
else
    echo -e "${YELLOW}⚠️  Maven Surefire plugin may need manual configuration${NC}"
fi

# Check 4: angular.json has karmaConfig
echo ""
echo "⚙️  Checking angular.json Karma configuration..."
if grep -q "karmaConfig" buy-01-ui/angular.json; then
    echo -e "${GREEN}✅ angular.json references karma.conf.js${NC}"
else
    echo -e "${RED}❌ angular.json missing karmaConfig reference${NC}"
    ISSUES=$((ISSUES + 1))
fi

# Check 5: karma.conf.js exists
echo ""
echo "📄 Checking karma.conf.js..."
if [ -f "buy-01-ui/karma.conf.js" ]; then
    echo -e "${GREEN}✅ karma.conf.js exists${NC}"
    
    # Check for reporters
    if grep -q "junitReporter" buy-01-ui/karma.conf.js; then
        echo -e "${GREEN}   ✅ JUnit reporter configured${NC}"
    else
        echo -e "${YELLOW}   ⚠️  JUnit reporter not found${NC}"
    fi
    
    if grep -q "coverageReporter" buy-01-ui/karma.conf.js; then
        echo -e "${GREEN}   ✅ Coverage reporter configured${NC}"
    else
        echo -e "${YELLOW}   ⚠️  Coverage reporter not found${NC}"
    fi
else
    echo -e "${RED}❌ karma.conf.js not found${NC}"
    ISSUES=$((ISSUES + 1))
fi

# Check 6: npm dependencies installed
echo ""
echo "📚 Checking npm dependencies..."
cd buy-01-ui
if npm ls karma-junit-reporter &>/dev/null; then
    echo -e "${GREEN}✅ karma-junit-reporter installed${NC}"
else
    echo -e "${RED}❌ karma-junit-reporter NOT installed${NC}"
    echo "   Run: npm install --save-dev karma-junit-reporter"
    ISSUES=$((ISSUES + 1))
fi

if npm ls karma-coverage &>/dev/null; then
    echo -e "${GREEN}✅ karma-coverage installed${NC}"
else
    echo -e "${RED}❌ karma-coverage NOT installed${NC}"
    echo "   Run: npm install --save-dev karma-coverage"
    ISSUES=$((ISSUES + 1))
fi
cd - &>/dev/null

# Check 7: Jenkinsfile frontend test creates directory
echo ""
echo "📁 Checking Jenkinsfile frontend test setup..."
if grep -q "mkdir -p target/surefire-reports" Jenkinsfile; then
    echo -e "${GREEN}✅ Frontend test directory creation configured${NC}"
else
    echo -e "${YELLOW}⚠️  Frontend test directory may not be created automatically${NC}"
fi

# Summary
echo ""
echo "======================================"
if [ $ISSUES -eq 0 ]; then
    echo -e "${GREEN}✅ ALL CHECKS PASSED!${NC}"
    echo "======================================"
    echo ""
    echo "Next steps:"
    echo "1. Run: mvn test (to verify backend coverage)"
    echo "2. Run: cd buy-01-ui && npm test (to verify frontend reporting)"
    echo "3. Trigger a Jenkins build to test full pipeline"
    echo "4. Check Jenkins UI for test reports and coverage links"
    exit 0
else
    echo -e "${RED}❌ Found $ISSUES issue(s)${NC}"
    echo "======================================"
    echo ""
    echo "Please fix the issues above and re-run this script."
    exit 1
fi
