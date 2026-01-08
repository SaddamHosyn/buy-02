#!/bin/bash
set -e

# Update system
dnf update -y

# Install Docker
dnf install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

# Create application directory
mkdir -p /home/ec2-user/buy-01-app
chown ec2-user:ec2-user /home/ec2-user/buy-01-app

# Set up cleanup cron job (runs every hour)
mkdir -p /etc/cron.d
cat > /etc/cron.d/docker-cleanup <<'EOF'
0 * * * * root docker image prune -a -f --filter "until=1h" && docker builder prune -f --filter "until=1h" && docker volume prune -f
EOF

# Create marker file to indicate setup is complete
touch /var/lib/cloud/instance/setup-complete

echo "Deployment server setup completed successfully!"
