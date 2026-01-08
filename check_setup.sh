#!/bin/bash
echo "Checking setup status..."
echo ""
echo "Jenkins (13.62.141.159):"
ssh -i ~/Downloads/lastreal.pem -o StrictHostKeyChecking=no -o ConnectTimeout=5 ec2-user@13.62.141.159 'test -f /var/lib/cloud/instance/setup-complete && echo "✅ Ready!" || echo "⏳ Still installing..."' 2>&1 | grep -v "Warning:"
echo ""
echo "Deployment (13.61.234.232):"
ssh -i ~/Downloads/lastreal.pem -o StrictHostKeyChecking=no -o ConnectTimeout=5 ec2-user@13.61.234.232 'test -f /var/lib/cloud/instance/setup-complete && echo "✅ Ready!" || echo "⏳ Still installing..."' 2>&1 | grep -v "Warning:"
