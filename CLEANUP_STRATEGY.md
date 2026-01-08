# Disk Space Cleanup Strategy

## Current Disk Usage (as of Build #6)

### Deployment Server (13.61.234.232)

- **Total**: 20GB
- **Used**: 6.3GB (32%)
- **Available**: 14GB
- **Docker Images**: 4.3GB (1.5GB reclaimable)

### Jenkins Server (13.62.141.159)

- **Total**: 20GB
- **Used**: 7.7GB (39%)
- **Available**: 13GB
- **Docker Images**: 1.9GB (100% reclaimable - no active containers)

---

## Automated Cleanup Mechanisms

### ✅ 1. Per-Build Cleanup (Real-time)

**Jenkins Server** - During every build:

```bash
# Pre-deployment (before building images)
docker image prune -a -f --filter "until=30m"
docker builder prune -f

# Post-deployment (after successful deploy)
docker image prune -a -f --filter "until=30m"
docker builder prune -f --filter "until=30m"
docker volume prune -f
```

**Deployment Server** - After every deployment:

```bash
docker image prune -a -f --filter "until=1h"
docker builder prune -f --filter "until=1h"
docker volume prune -f
```

### ✅ 2. Daily Automated Cleanup (Scheduled)

**Deployment Server** - Daily at 2:00 AM:

```cron
0 2 * * * root docker image prune -a -f --filter "until=24h" && \
                docker builder prune -a -f && \
                docker volume prune -f && \
                logger "Docker cleanup completed"
```

- Location: `/etc/cron.d/docker-cleanup`
- Removes images older than 24 hours
- Cleans all build cache
- Removes unused volumes

**Jenkins Server** - Daily at 3:00-3:30 AM:

```cron
# Workspace cleanup
0  3 * * * jenkins find /var/lib/jenkins/workspace -type d -name "target" -mtime +7 -exec rm -rf {} +
5  3 * * * jenkins find /var/lib/jenkins/workspace -type d -name "node_modules" -mtime +7 -exec rm -rf {} +
10 3 * * * jenkins find /var/lib/jenkins/workspace -type d -name "dist" -mtime +7 -exec rm -rf {} +

# Docker cleanup
30 3 * * * root docker image prune -a -f --filter "until=48h" && \
                 docker builder prune -a -f && \
                 logger "Jenkins Docker cleanup completed"
```

- Location: `/etc/cron.d/jenkins-cleanup`
- Removes Maven `target/` folders older than 7 days
- Removes npm `node_modules/` folders older than 7 days
- Removes Angular `dist/` folders older than 7 days
- Removes Docker images older than 48 hours

### ✅ 3. Version Backup Strategy

**Deployment Server** maintains:

- `latest` - Current running version
- `previous` - Last working version (for quick rollback)
- `build-N` - Tagged builds by number

Old `previous-old` backups are removed after successful deployments.

---

## What Gets Cleaned Up

### Immediate (Per-Build)

- ✅ Dangling images (no tags)
- ✅ Intermediate build layers not used by any image
- ✅ Build cache older than 30 minutes (Jenkins) / 1 hour (Deployment)
- ✅ Unused volumes

### Daily (Scheduled)

- ✅ All unused images older than 24h (Deployment) / 48h (Jenkins)
- ✅ All build cache
- ✅ Maven build artifacts older than 7 days
- ✅ npm dependencies older than 7 days
- ✅ Angular build outputs older than 7 days

### Never Cleaned (Protected)

- ❌ Currently running containers
- ❌ Images tagged as `latest`
- ❌ Images tagged as `previous`
- ❌ Images from the last 24-48 hours

---

## Disk Space Projections

### Current Build Size Per Deployment

- Backend images: ~500MB × 5 = 2.5GB
- Frontend image: ~55MB
- Base images (Java, Node, nginx): ~400MB (cached)
- **Total per build**: ~3GB

### With Current Cleanup Strategy

- **Maximum builds stored**: Current + Previous = 2 builds
- **Expected usage**: 6-8GB for Docker images
- **Remaining space**: 12-14GB free (sufficient)

### Capacity Planning

With 20GB disks and automated cleanup:

- ✅ Can handle **6-8 builds** worth of images simultaneously
- ✅ Cron jobs prevent long-term accumulation
- ✅ Per-build cleanup provides immediate space recovery
- ✅ Safe margin: 12-14GB free space maintained

---

## Manual Cleanup Commands

### Emergency Space Recovery (Run on any server)

```bash
# See what can be reclaimed
docker system df

# Aggressive cleanup (keeps only running containers)
sudo docker system prune -a -f

# Nuclear option (WARNING: Stops all containers and removes everything)
sudo docker stop $(docker ps -aq)
sudo docker system prune -a -f --volumes
```

### Check Disk Usage

```bash
# Overall disk usage
df -h /

# Docker-specific usage
docker system df

# Detailed breakdown
docker images
docker ps -a
docker volume ls
```

### Manual Image Cleanup

```bash
# Remove specific old build
docker rmi buy01-pipeline-frontend:build-3

# Remove all untagged images
docker rmi $(docker images -f "dangling=true" -q)

# Remove old builds (keep last 2)
docker images | grep "buy01-pipeline" | grep "build-" | tail -n +3 | awk '{print $1":"$2}' | xargs docker rmi
```

---

## Monitoring Recommendations

### Weekly Health Check

```bash
# Deployment server
ssh ec2-user@13.61.234.232 'df -h / && docker system df'

# Jenkins server
ssh ec2-user@13.62.141.159 'df -h / && docker system df'
```

### Warning Thresholds

- ⚠️ **80% disk usage**: Review and run manual cleanup
- 🚨 **90% disk usage**: Emergency cleanup required
- ✅ **<60% disk usage**: Healthy

### Cron Job Verification

```bash
# Check if cron jobs are running
sudo grep docker /var/log/cron
sudo grep -i "cleanup completed" /var/log/messages
```

---

## Future Improvements

1. **Monitoring Dashboard**: Set up CloudWatch alarms for disk usage >80%
2. **S3 Archival**: Archive old Docker images to S3 for disaster recovery
3. **Elastic Storage**: Consider resizing to 30GB if frequent large builds
4. **Automated Alerts**: Email notifications when disk usage exceeds thresholds
5. **Jenkins Plugin**: Install "Disk Usage Plugin" for visual monitoring

---

## Summary

✅ **Problem Solved**: With automated daily cleanup + per-build cleanup, you won't hit disk space issues again.

**Cleanup happens:**

- **Every build**: Removes images >30min old (Jenkins), >1h old (Deployment)
- **Every day**: Removes images >24h old (Deployment), >48h old (Jenkins)
- **Every week**: Removes build artifacts >7 days old (Jenkins workspaces)

**Current status:** 32% (Deployment) and 39% (Jenkins) disk usage - plenty of headroom! 🎯
