#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

if [ $# -ne 2 ]; then
    echo "Usage: $0 <new-jenkins-ip> <new-deployment-ip>"
    echo "Example: $0 13.60.233.212 16.170.204.134"
    exit 1
fi

NEW_JENKINS_IP=$1
NEW_DEPLOYMENT_IP=$2
OLD_JENKINS_IP="13.60.233.212"
OLD_DEPLOYMENT_IP="16.170.204.134"
KEY_PATH="$HOME/Downloads/lastreal.pem"

echo -e "${BLUE}=========================================="
echo "Migration to New Servers"
echo -e "==========================================${NC}\n"

echo -e "${YELLOW}New Jenkins IP: ${NEW_JENKINS_IP}${NC}"
echo -e "${YELLOW}New Deployment IP: ${NEW_DEPLOYMENT_IP}${NC}\n"

# Wait for instances to be ready
echo -e "${YELLOW}[1/6] Waiting for instances to complete setup (this may take 5-10 minutes)...${NC}"
for i in {1..60}; do
    if ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no -o ConnectTimeout=5 ec2-user@${NEW_JENKINS_IP} "test -f /var/lib/cloud/instance/setup-complete" 2>/dev/null; then
        echo -e "${GREEN}✓ Jenkins server ready${NC}"
        break
    fi
    echo "Waiting for Jenkins setup to complete... ($i/60)"
    sleep 10
done

for i in {1..60}; do
    if ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no -o ConnectTimeout=5 ec2-user@${NEW_DEPLOYMENT_IP} "test -f /var/lib/cloud/instance/setup-complete" 2>/dev/null; then
        echo -e "${GREEN}✓ Deployment server ready${NC}"
        break
    fi
    echo "Waiting for Deployment setup to complete... ($i/60)"
    sleep 10
done

# Backup old Jenkins config
echo -e "\n${YELLOW}[2/6] Backing up Jenkins configuration from old server...${NC}"
ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no ec2-user@${OLD_JENKINS_IP} "sudo tar -czf /tmp/jenkins-backup.tar.gz -C /var/lib/jenkins jobs users config.xml hudson.tasks.Mailer.xml credentials.xml 2>/dev/null || true"
scp -i "$KEY_PATH" -o StrictHostKeyChecking=no ec2-user@${OLD_JENKINS_IP}:/tmp/jenkins-backup.tar.gz /tmp/
echo -e "${GREEN}✓ Jenkins backup downloaded${NC}"

# Restore Jenkins config to new server
echo -e "\n${YELLOW}[3/6] Restoring Jenkins configuration to new server...${NC}"
scp -i "$KEY_PATH" -o StrictHostKeyChecking=no /tmp/jenkins-backup.tar.gz ec2-user@${NEW_JENKINS_IP}:/tmp/
ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no ec2-user@${NEW_JENKINS_IP} <<'ENDSSH'
sudo systemctl stop jenkins
sudo tar -xzf /tmp/jenkins-backup.tar.gz -C /var/lib/jenkins/ 2>/dev/null || true
sudo chown -R jenkins:jenkins /var/lib/jenkins
sudo systemctl start jenkins
ENDSSH
echo -e "${GREEN}✓ Jenkins configuration restored${NC}"

# Update deploy.sh with new IP
echo -e "\n${YELLOW}[4/6] Updating deploy.sh with new deployment IP...${NC}"
cd /Users/othmane.afilali/Desktop/mr-jenk
sed -i.bak "s|DEPLOY_HOST=\"ec2-user@.*\"|DEPLOY_HOST=\"ec2-user@${NEW_DEPLOYMENT_IP}\"|g" deploy.sh
sed -i.bak "s|AWS_PUBLIC_IP=\".*\"|AWS_PUBLIC_IP=\"${NEW_DEPLOYMENT_IP}\"|g" deploy.sh
echo -e "${GREEN}✓ deploy.sh updated${NC}"

# Update README with new IPs
echo -e "\n${YELLOW}[5/6] Updating README.md with new IPs...${NC}"
sed -i.bak "s|http://[0-9.]*:4200|http://${NEW_DEPLOYMENT_IP}:4200|g" README.md
sed -i.bak "s|http://[0-9.]*:8080|http://${NEW_DEPLOYMENT_IP}:8080|g" README.md
sed -i.bak "s|http://[0-9.]*:8761|http://${NEW_DEPLOYMENT_IP}:8761|g" README.md
sed -i.bak "s|Jenkins: http://[0-9.]*:8080|Jenkins: http://${NEW_JENKINS_IP}:8080|g" README.md
echo -e "${GREEN}✓ README.md updated${NC}"

# Commit changes
echo -e "\n${YELLOW}[6/6] Committing changes and triggering deployment...${NC}"
git add deploy.sh README.md
git commit -m "Update IPs for new infrastructure: Jenkins ${NEW_JENKINS_IP}, Deployment ${NEW_DEPLOYMENT_IP}"
git push
echo -e "${GREEN}✓ Changes pushed to GitHub${NC}"

echo -e "\n${BLUE}=========================================="
echo "Migration Complete!"
echo -e "==========================================${NC}\n"

echo -e "${GREEN}New URLs:${NC}"
echo -e "Jenkins: http://${NEW_JENKINS_IP}:8080"
echo -e "Frontend: http://${NEW_DEPLOYMENT_IP}:4200"
echo -e "API Gateway: http://${NEW_DEPLOYMENT_IP}:8080"
echo -e "Eureka: http://${NEW_DEPLOYMENT_IP}:8761"

echo -e "\n${YELLOW}Next Steps:${NC}"
echo "1. Access Jenkins at http://${NEW_JENKINS_IP}:8080"
echo "2. Get initial admin password:"
echo "   ssh -i ~/Downloads/lastreal.pem ec2-user@${NEW_JENKINS_IP} 'sudo cat /var/lib/jenkins/secrets/initialAdminPassword'"
echo "3. Wait 1-2 minutes for Jenkins to detect the git push"
echo "4. Monitor build at http://${NEW_JENKINS_IP}:8080/job/buy01-pipeline/"
echo ""
echo -e "${GREEN}You can now terminate the old instances if everything works!${NC}"
