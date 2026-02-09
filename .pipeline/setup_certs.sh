#!/bin/bash
set -e

VOL_NAME="buy01-certs"
FORCE=${1:-false}

echo "Checking certificate status for volume: $VOL_NAME"

# Check if certificates already exist
if [ "$FORCE" != "--force" ] && docker volume inspect $VOL_NAME > /dev/null 2>&1; then
    if docker run --rm -v $VOL_NAME:/data alpine ls /data/localhost.p12 > /dev/null 2>&1; then
        echo "✅ Certificates already exist in volume '$VOL_NAME'."
        echo "   Use './setup_certs.sh --force' to regenerate if needed."
        exit 0
    fi
fi

echo "Setting up certificates..."

# Create volume if it doesn't exist
docker volume create $VOL_NAME > /dev/null

# Create temp directory for generation
mkdir -p .tmp_certs
cd .tmp_certs

echo "Generating self-signed certificate..."
# Generate key and certificate (PEM format)
openssl req -x509 -out localhost.pem -keyout localhost-key.pem \
  -newkey rsa:2048 -nodes -sha256 \
  -subj '/CN=localhost' -days 3650 \
  -extensions EXT -config <( \
   printf "[dn]\nCN=localhost\n[req]\ndistinguished_name = dn\n[EXT]\nsubjectAltName=DNS:localhost\nkeyUsage=digitalSignature\nextendedKeyUsage=serverAuth")

echo "Generating PKCS12 keystore..."
# Generate PKCS12 keystore (for Java)
openssl pkcs12 -export -in localhost.pem -inkey localhost-key.pem \
  -out localhost.p12 -name api-gateway \
  -password pass:changeit

echo "Copying certificates to volume..."
# Copy to Docker volume using a temporary helper container
docker run --rm -v $VOL_NAME:/dest -v "$(pwd)":/source alpine \
  sh -c "cp /source/localhost.pem /dest/ && cp /source/localhost-key.pem /dest/ && cp /source/localhost.p12 /dest/ && chmod 644 /dest/*"

# Cleanup
cd ..
rm -rf .tmp_certs

echo "✅ Certificates setup complete."
echo "   - Volume: $VOL_NAME"
echo "   - Files: localhost.pem, localhost-key.pem, localhost.p12"
