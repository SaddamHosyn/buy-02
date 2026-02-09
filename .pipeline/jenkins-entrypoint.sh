#!/bin/bash
# Jenkins custom entrypoint to fix Docker socket permissions

# Get the GID of the mounted docker socket
DOCKER_SOCK_GID=$(stat -c '%g' /var/run/docker.sock 2>/dev/null || stat -f '%g' /var/run/docker.sock 2>/dev/null || echo 0)

# If docker group doesn't exist with that GID, modify it
if [ "$DOCKER_SOCK_GID" != "0" ]; then
    # Check if a group with this GID already exists
    if ! getent group "$DOCKER_SOCK_GID" > /dev/null 2>&1; then
        # Modify docker group to match the socket GID
        groupmod -g "$DOCKER_SOCK_GID" docker 2>/dev/null || true
    else
        # Add jenkins to the existing group with that GID
        EXISTING_GROUP=$(getent group "$DOCKER_SOCK_GID" | cut -d: -f1)
        usermod -aG "$EXISTING_GROUP" jenkins 2>/dev/null || true
    fi
    
    # Ensure jenkins is in docker group
    usermod -aG docker jenkins 2>/dev/null || true
fi

# Ensure socket is readable/writable
chmod 666 /var/run/docker.sock 2>/dev/null || true

# Call the original Jenkins entrypoint
exec /usr/local/bin/jenkins.sh "$@"
