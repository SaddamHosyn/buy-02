#!/bin/bash

echo "🧪 Testing CORS Configuration..."
echo ""

# Test OPTIONS preflight request (this was failing before)
echo "1️⃣  Testing OPTIONS (preflight) request to /api/auth/login:"
curl -X OPTIONS http://51.21.198.139:8080/api/auth/login \
  -H "Origin: http://51.21.198.139:4200" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v -o /dev/null 2>&1 | grep -E "< HTTP|< access-control"

echo ""
echo "2️⃣  Expected Response:"
echo "   ✅ HTTP/1.1 200 OK"
echo "   ✅ access-control-allow-origin: http://51.21.198.139:4200"
echo "   ✅ access-control-allow-methods: ..."
echo ""
echo "If you see '403 Forbidden', the fix hasn't deployed yet. Wait a few more minutes."

