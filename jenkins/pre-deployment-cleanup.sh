#!/bin/bash

# Deployment Pre-cleanup Script
# Removes old Docker images and builder cache to free disk space

set -e

echo "Pre-deployment cleanup to free disk space..."

# Remove ALL old build-tagged images (keep only what we're about to build)
echo "Removing all old build-tagged images..."
docker images --format "{{.Repository}}:{{.Tag}}" | grep "buy01-pipeline.*:build-" | xargs -r docker rmi -f || true

# General cleanup of old images
docker image prune -a -f --filter "until=30m"
docker builder prune -f

# Show remaining disk space
echo "Disk space after cleanup:"
df -h /var/lib/docker | tail -1
echo "✓ Pre-deployment cleanup completed"
