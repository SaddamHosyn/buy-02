#!/bin/bash
set -e

# Update system
dnf update -y

# Install Git
dnf install -y git

# Install Java 17
dnf install -y java-17-amazon-corretto-devel

# Install Maven
wget https://dlcdn.apache.org/maven/maven-3/3.9.12/binaries/apache-maven-3.9.12-bin.tar.gz -O /tmp/maven.tar.gz
tar -xzf /tmp/maven.tar.gz -C /opt
ln -s /opt/apache-maven-3.9.12 /opt/maven
echo 'export M2_HOME=/opt/maven' >> /etc/profile.d/maven.sh
echo 'export PATH=$M2_HOME/bin:$PATH' >> /etc/profile.d/maven.sh
chmod +x /etc/profile.d/maven.sh

# Install Node.js 20
dnf install -y nodejs npm

# Install Angular CLI globally
npm install -g @angular/cli

# Install Docker
dnf install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# Install Jenkins
wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key
dnf install -y jenkins

# Configure Jenkins JVM (6GB heap)
mkdir -p /etc/systemd/system/jenkins.service.d
cat > /etc/systemd/system/jenkins.service.d/override.conf <<'EOF'
[Service]
Environment="JAVA_OPTS=-Djava.awt.headless=true -Xms3g -Xmx6g -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
EOF

# Add Jenkins to docker group
usermod -aG docker jenkins

# Start Jenkins
systemctl daemon-reload
systemctl start jenkins
systemctl enable jenkins

# Create marker file to indicate setup is complete
touch /var/lib/cloud/instance/setup-complete

echo "Jenkins setup completed successfully!"
