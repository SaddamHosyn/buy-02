#!/bin/bash

# Post-deployment cleanup script
# Aggressive cleanup on Jenkins to save disk space

set -e

echo "Post-deployment cleanup on Jenkins..."
docker image prune -a -f --filter "until=30m"
docker builder prune -f --filter "until=30m"
docker volume prune -f
echo "✓ Jenkins cleanup completed"
