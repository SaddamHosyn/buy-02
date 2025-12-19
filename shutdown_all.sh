#!/usr/bin/env bash
set -euo pipefail

# shutdown_all.sh
# Safely stop this project's services.
# Usage:
#   ./shutdown_all.sh          -> runs `docker compose down --remove-orphans`
#   ./shutdown_all.sh --cleanup -> also removes local images and volumes created by compose
#   ./shutdown_all.sh --help   -> show this help

PRINT_USAGE(){
  cat <<EOF
Usage: $0 [--cleanup] [--help]

Options:
  --cleanup   Also remove local images and volumes (docker compose down --rmi local --volumes)
  --help      Show this help

This script will run docker compose down in the current project directory.
It does NOT stop or modify host-managed services (e.g. Homebrew services) unless you run cleanup and knowingly remove volumes.
EOF
}

if [[ ${1:-} == "--help" ]]; then
  PRINT_USAGE
  exit 0
fi

CLEANUP=false
if [[ ${1:-} == "--cleanup" ]]; then
  CLEANUP=true
fi

echo "Stopping Docker Compose stack in $(pwd)"

if $CLEANUP; then
  echo "Running docker compose down --rmi local --volumes --remove-orphans"
  docker compose down --rmi local --volumes --remove-orphans
else
  echo "Running docker compose down --remove-orphans"
  docker compose down --remove-orphans
fi

echo "Done."

exit 0
