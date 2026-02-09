#!/bin/bash

# Docker Build Script
# Builds all Docker images with proper tagging

set -e

BUILD_NUMBER=${1:?'BUILD_NUMBER is required. Usage: build-docker-images.sh <BUILD_NUMBER>'}

echo "Building Docker images with build #${BUILD_NUMBER}..."

# Build backend services with build number tags
docker build -t buy01-pipeline-service-registry:build-${BUILD_NUMBER} ./service-registry
docker build -t buy01-pipeline-api-gateway:build-${BUILD_NUMBER} ./api-gateway
docker build -t buy01-pipeline-user-service:build-${BUILD_NUMBER} ./user-service
docker build -t buy01-pipeline-product-service:build-${BUILD_NUMBER} ./product-service
docker build -t buy01-pipeline-order-service:build-${BUILD_NUMBER} ./order-service
docker build -t buy01-pipeline-media-service:build-${BUILD_NUMBER} ./media-service

# Build frontend with build number tag (--no-cache forces complete rebuild)
docker build --no-cache -t buy01-pipeline-frontend:build-${BUILD_NUMBER} ./buy-01-ui

# Also tag as latest for compatibility
docker tag buy01-pipeline-service-registry:build-${BUILD_NUMBER} buy01-pipeline-service-registry:latest
docker tag buy01-pipeline-api-gateway:build-${BUILD_NUMBER} buy01-pipeline-api-gateway:latest
docker tag buy01-pipeline-user-service:build-${BUILD_NUMBER} buy01-pipeline-user-service:latest
docker tag buy01-pipeline-product-service:build-${BUILD_NUMBER} buy01-pipeline-product-service:latest
docker tag buy01-pipeline-order-service:build-${BUILD_NUMBER} buy01-pipeline-order-service:latest
docker tag buy01-pipeline-media-service:build-${BUILD_NUMBER} buy01-pipeline-media-service:latest
docker tag buy01-pipeline-frontend:build-${BUILD_NUMBER} buy01-pipeline-frontend:latest

echo "✓ All Docker images built with build-${BUILD_NUMBER} tags"
