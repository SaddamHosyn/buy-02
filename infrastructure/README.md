# Infrastructure Automation

This directory contains Terraform configuration to automatically provision and configure AWS infrastructure for the Buy-01 application.

## What This Does

**Automatically provisions:**

- ✅ Jenkins server (`t7i-flex.large` - 2 vCPU, 8GB RAM)
- ✅ Deployment server (`m7i-flex.large` - 2 vCPU, 7.6GB RAM)
- ✅ Static IPs (Elastic IPs - no more IP changes!)
- ✅ Security groups with proper ports
- ✅ 20GB storage on each instance

**Automatically installs:**

- Jenkins: Git, Maven, Node.js, Angular CLI, Docker, Jenkins with 6GB heap
- Deployment: Docker, Docker Compose, cleanup cron jobs

## Prerequisites

1. **AWS CLI configured** with credentials:

   ```bash
   aws configure
   ```

2. **Terraform installed** (if not installed):

   ```bash
   brew install terraform
   ```

3. **SSH key pair** `lastreal` must exist in AWS eu-north-1 region

## Usage

### Step 1: Provision Infrastructure

```bash
cd infrastructure
terraform init
terraform plan
terraform apply
```

**This will take 2-3 minutes** and output the new static IPs.

### Step 2: Wait for Setup

Wait 5-10 minutes for instances to complete automated setup (installing software).

### Step 3: Migrate from Old Servers

Run the migration script with the new IPs from terraform output:

```bash
chmod +x migrate-to-new-servers.sh
./migrate-to-new-servers.sh <new-jenkins-ip> <new-deployment-ip>
```

This script will:

1. Wait for instances to be ready
2. Backup Jenkins config from old server
3. Restore to new Jenkins server
4. Update deploy.sh with new IP
5. Update README.md with new IPs
6. Commit and push changes (triggers deployment)

### Step 4: Verify

1. Access Jenkins: `http://<new-jenkins-ip>:8080`
2. Monitor deployment build
3. Access frontend: `http://<new-deployment-ip>:4200`

### Step 5: Cleanup Old Instances

Once everything works on new instances:

1. Go to AWS Console > EC2 > Instances
2. Terminate old instances (13.60.233.212 and 16.170.204.134)
3. Release old Elastic IPs to avoid charges

## Terraform Commands

```bash
# View planned changes
terraform plan

# Apply changes
terraform apply

# Destroy infrastructure (when done)
terraform destroy

# View current infrastructure
terraform show

# View outputs again
terraform output
```

## Benefits

- ✅ **Better Performance**: t7i-flex.large has better CPU than m7i-flex.large
- ✅ **Static IPs**: No more IP changes when restarting instances
- ✅ **Automated Setup**: No manual installation needed
- ✅ **Reproducible**: Can recreate infrastructure anytime with one command
- ✅ **Infrastructure as Code**: All configuration tracked in Git

## Cost Estimation

- Both instances: Within free tier (750 hours/month each)
- Elastic IPs: Free when attached to running instances
- Storage: 40GB total (within 30GB free tier + $0.10/GB/month for extra 10GB = ~$1/month)

**Estimated cost: $1-2/month** (mostly for extra storage beyond 30GB free tier)

## Troubleshooting

**If instances aren't ready after 10 minutes:**

```bash
ssh -i ~/Downloads/lastreal.pem ec2-user@<ip> 'tail -f /var/log/cloud-init-output.log'
```

**If Jenkins password needed:**

```bash
ssh -i ~/Downloads/lastreal.pem ec2-user@<jenkins-ip> 'sudo cat /var/lib/jenkins/secrets/initialAdminPassword'
```

**Check instance status:**

```bash
aws ec2 describe-instances --filters "Name=tag:Name,Values=buy01-*" --query 'Reservations[*].Instances[*].[Tags[?Key==`Name`].Value|[0],State.Name,PublicIpAddress]' --output table
```
