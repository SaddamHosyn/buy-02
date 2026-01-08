# Jenkins Security Configuration Guide

## 🔒 Critical Security Setup Required

This guide walks you through securing your Jenkins instance properly.

---

## 1. Jenkins Dashboard Permissions Setup

### Initial Admin Setup
1. **First Login:**
   ```bash
   # Get initial admin password
   sudo cat /var/lib/jenkins/secrets/initialAdminPassword
   ```

2. **Install Required Plugins:**
   - Credentials Plugin
   - Credentials Binding Plugin
   - SSH Agent Plugin
   - Role-based Authorization Strategy

### Configure Security Realm

1. Navigate to: `Manage Jenkins` → `Security` → `Configure Global Security`

2. **Security Realm:** Select "Jenkins' own user database"
   - ✅ Check "Allow users to sign up" (initially)
   - Create admin accounts
   - ⚠️ **IMPORTANT:** Uncheck "Allow users to sign up" after creating accounts

3. **Authorization Strategy:** Select "Matrix-based security" or "Role-Based Strategy"

### Recommended Permission Matrix

**For Matrix-based security:**

| Permission | Admin | Developer | Viewer |
|------------|-------|-----------|--------|
| Overall/Administer | ✅ | ❌ | ❌ |
| Overall/Read | ✅ | ✅ | ✅ |
| Job/Build | ✅ | ✅ | ❌ |
| Job/Cancel | ✅ | ✅ | ❌ |
| Job/Configure | ✅ | ✅ | ❌ |
| Job/Create | ✅ | ✅ | ❌ |
| Job/Delete | ✅ | ❌ | ❌ |
| Job/Read | ✅ | ✅ | ✅ |
| Job/Workspace | ✅ | ✅ | ❌ |
| Credentials/View | ✅ | ❌ | ❌ |
| Credentials/Create | ✅ | ❌ | ❌ |
| Credentials/Update | ✅ | ❌ | ❌ |
| Credentials/Delete | ✅ | ❌ | ❌ |

**Setup Steps:**
1. Go to `Manage Jenkins` → `Configure Global Security`
2. Under **Authorization**, select `Matrix-based security`
3. Add users: `ozzy`, `jedi`, `viewer`
4. Assign permissions as per table above
5. Click **Save**

---

## 2. Jenkins Credentials Store Setup

### Required Credentials to Add

#### A. SSH Private Key for AWS Deployment
1. Navigate to: `Manage Jenkins` → `Manage Credentials` → `(global)` → `Add Credentials`
2. **Kind:** SSH Username with private key
3. **Scope:** Global
4. **ID:** `aws-deploy-ssh-key`
5. **Description:** AWS EC2 SSH Key for Deployment
6. **Username:** `ec2-user`
7. **Private Key:** Enter directly (paste content of `lastreal.pem`)
8. Click **OK**

#### B. AWS Deployment Host IP
1. Add Credentials
2. **Kind:** Secret text
3. **Scope:** Global
4. **Secret:** `13.61.234.232` (your AWS IP)
5. **ID:** `aws-deploy-host-ip`
6. **Description:** AWS EC2 Deployment Host IP
7. Click **OK**

#### C. MongoDB Root Password
1. Add Credentials
2. **Kind:** Secret text
3. **Scope:** Global
4. **Secret:** (generate strong password)
5. **ID:** `mongodb-root-password`
6. **Description:** MongoDB Root Password
7. Click **OK**

**Generate Strong Password:**
```bash
openssl rand -base64 32
```

#### D. Team Email Addresses
1. Add Credentials (Ozzy Email)
2. **Kind:** Secret text
3. **Scope:** Global
4. **Secret:** `othmane.afilali@gritlab.ax`
5. **ID:** `email-ozzy`
6. **Description:** Ozzy Email Address
7. Click **OK**

8. Repeat for Jedi:
   - **Secret:** `jedi.reston@gritlab.ax`
   - **ID:** `email-jedi`
   - **Description:** Jedi Email Address

#### E. Docker Registry Credentials (if using private registry)
1. Add Credentials
2. **Kind:** Username with password
3. **Scope:** Global
4. **ID:** `docker-registry-credentials`
5. **Username:** (your registry username)
6. **Password:** (your registry password)
7. Click **OK**

---

## 3. SSH Host Key Verification

### Configure Known Hosts (Secure Alternative to StrictHostKeyChecking=no)

```bash
# On Jenkins server, as jenkins user
sudo su - jenkins

# Add AWS host to known_hosts
ssh-keyscan -H 13.61.234.232 >> ~/.ssh/known_hosts

# Verify
ssh-keygen -H -F 13.61.234.232
```

---

## 4. Environment Variables Security

### Jenkins Global Environment Variables
1. Navigate to: `Manage Jenkins` → `Configure System`
2. Scroll to **Global properties**
3. Check **Environment variables**
4. Add ONLY non-sensitive variables here:
   - `DEPLOY_PATH=/home/ec2-user/buy-01-app`
   - `DOCKER_IMAGE_PREFIX=buy01-pipeline`

⚠️ **NEVER store passwords, keys, or tokens here!**

---

## 5. Script Approval

### Configure Script Security
1. Navigate to: `Manage Jenkins` → `In-process Script Approval`
2. Review and approve ONLY trusted scripts
3. **Best Practice:** Minimize Groovy scripts in Jenkinsfile

---

## 6. Audit and Monitoring

### Enable Audit Trail
1. Install plugin: **Audit Trail Plugin**
2. Navigate to: `Manage Jenkins` → `Configure System`
3. Find **Audit Trail** section
4. Configure log location: `/var/lib/jenkins/logs/audit.log`
5. Log pattern:
   ```
   [%d{yyyy-MM-dd HH:mm:ss}] %m%n
   ```
6. Click **Save**

### Review Logs Regularly
```bash
# View audit logs
sudo tail -f /var/lib/jenkins/logs/audit.log

# View Jenkins logs
sudo journalctl -u jenkins -f
```

---

## 7. Network Security

### Firewall Configuration
```bash
# Allow only necessary ports
sudo firewall-cmd --permanent --add-port=8080/tcp  # Jenkins UI
sudo firewall-cmd --permanent --add-port=22/tcp    # SSH
sudo firewall-cmd --reload
```

### Restrict Jenkins Access by IP (Optional)
```bash
# Edit Jenkins config
sudo vi /etc/sysconfig/jenkins

# Add allowed IP ranges
JENKINS_ARGS="--httpListenAddress=127.0.0.1"

# Then use nginx reverse proxy with IP whitelist
```

---

## 8. SSL/TLS Configuration (Recommended)

### Option 1: Nginx Reverse Proxy with Let's Encrypt
```bash
# Install nginx and certbot
sudo dnf install -y nginx certbot python3-certbot-nginx

# Configure nginx for Jenkins
sudo vi /etc/nginx/conf.d/jenkins.conf
```

```nginx
server {
    listen 80;
    server_name jenkins.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name jenkins.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/jenkins.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/jenkins.yourdomain.com/privkey.pem;

    # IP whitelist (optional)
    # allow 203.0.113.0/24;
    # deny all;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# Get SSL certificate
sudo certbot --nginx -d jenkins.yourdomain.com

# Restart nginx
sudo systemctl restart nginx
```

---

## 9. Regular Security Maintenance

### Weekly Tasks
- [ ] Review audit logs
- [ ] Check for failed login attempts
- [ ] Review user permissions

### Monthly Tasks
- [ ] Update Jenkins and plugins
- [ ] Rotate credentials
- [ ] Review and remove unused credentials
- [ ] Check disk space and logs

### Security Checklist
```bash
# Update Jenkins
sudo dnf update -y jenkins

# Update plugins via UI
# Manage Jenkins → Manage Plugins → Updates tab → Select all → Download and install

# Backup credentials
sudo tar -czf jenkins-creds-backup-$(date +%Y%m%d).tar.gz /var/lib/jenkins/credentials.xml
```

---

## 10. Backup Strategy for Credentials

```bash
# Backup Jenkins home (includes credentials)
sudo systemctl stop jenkins
sudo tar -czf jenkins-backup-$(date +%Y%m%d).tar.gz /var/lib/jenkins
sudo systemctl start jenkins

# Store backup securely (encrypted)
gpg --symmetric --cipher-algo AES256 jenkins-backup-*.tar.gz
```

---

## 🚨 Security Incident Response

### If Credentials are Compromised:
1. **Immediately** rotate all affected credentials
2. Review audit logs for unauthorized access
3. Check for unauthorized job modifications
4. Scan for malicious code in jobs
5. Update all credentials in Jenkins store
6. Update credentials on target systems (AWS, databases)

### Emergency Contacts:
- **Security Team:** security@company.com
- **Admin Team:** ozzy, jedi

---

## Verification Checklist

After completing this setup, verify:

- [ ] Jenkins dashboard requires authentication
- [ ] Non-admin users cannot access Manage Jenkins
- [ ] All credentials stored in Jenkins credentials store
- [ ] No hardcoded passwords in code
- [ ] SSH keys stored securely in Jenkins
- [ ] Audit logging enabled
- [ ] Regular backups scheduled
- [ ] SSL/TLS configured (recommended)
- [ ] Firewall rules configured
- [ ] Script approval configured

---

## Additional Resources

- [Jenkins Security Best Practices](https://www.jenkins.io/doc/book/security/)
- [Securing Jenkins](https://www.jenkins.io/doc/book/security/securing-jenkins/)
- [Managing Security](https://www.jenkins.io/doc/book/managing/security/)
