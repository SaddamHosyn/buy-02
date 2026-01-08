# 🚀 Quick Start - AWS Deployment

## ✅ Setup Complete!

Your project is now configured for automated AWS deployment. Changes have been pushed to trigger Jenkins.

## 📋 Final Checklist

### 1. AWS Security Group Configuration

Go to AWS Console → EC2 → Security Groups → Select your instance's security group

**Add these Inbound Rules:**

```
Type            Protocol    Port Range    Source
---------------------------------------------------
SSH             TCP         22            Your IP
HTTP            TCP         80            0.0.0.0/0
HTTPS           TCP         443           0.0.0.0/0
Custom TCP      TCP         4200          0.0.0.0/0
Custom TCP      TCP         4201          0.0.0.0/0
Custom TCP      TCP         8080          0.0.0.0/0
Custom TCP      TCP         8081          0.0.0.0/0
Custom TCP      TCP         8082          0.0.0.0/0
Custom TCP      TCP         8083          0.0.0.0/0
Custom TCP      TCP         8761          0.0.0.0/0
```

### 2. Jenkins Configuration

**Option A: Simple (If Jenkins is on the same machine)**
Your deploy.sh will use the SSH key from ~/Downloads/lastreal.pem

**Option B: If Jenkins is on AWS (Remote Server)**

```bash
# Copy SSH key to Jenkins server
scp ~/Downloads/lastreal.pem jenkins-user@your-jenkins-ip:/var/lib/jenkins/.ssh/
ssh jenkins-user@your-jenkins-ip
sudo chmod 600 /var/lib/jenkins/.ssh/lastreal.pem
sudo chown jenkins:jenkins /var/lib/jenkins/.ssh/lastreal.pem
```

### 3. Monitor Jenkins Pipeline

1. Go to your Jenkins dashboard
2. Find your job for "mr-jenk" repository
3. Watch the pipeline stages:
   - ✅ Checkout
   - ✅ Build Backend
   - ✅ Test Backend
   - ✅ Build Frontend
   - ✅ Test Frontend
   - 🚀 **Deploy** (NEW - will deploy to AWS!)

## 🌐 Access Your Application

Once Jenkins completes deployment (5-10 minutes first time):

| Service              | URL                        |
| -------------------- | -------------------------- |
| **Frontend (HTTPS)** | https://51.21.198.139:4201 |
| **Frontend (HTTP)**  | http://51.21.198.139:4200  |
| **API Gateway**      | http://51.21.198.139:8080  |
| **Eureka Dashboard** | http://51.21.198.139:8761  |
| **User Service**     | http://51.21.198.139:8081  |
| **Product Service**  | http://51.21.198.139:8082  |
| **Media Service**    | http://51.21.198.139:8083  |

## 🔍 Troubleshooting

### Check Jenkins Pipeline Status

Look for the Deploy stage in Jenkins console output for any errors.

### Check AWS Instance

```bash
# SSH into AWS
ssh -i ~/Downloads/lastreal.pem ec2-user@51.21.198.139

# Check running containers
sudo docker ps

# Check logs
cd /home/ec2-user/buy-01-app
sudo docker-compose logs -f

# Check specific service
sudo docker logs buy-01-api-gateway -f
```

### Common Issues

**1. "Permission denied" during deployment**

```bash
# Fix SSH key permissions
chmod 600 ~/Downloads/lastreal.pem
```

**2. "Cannot connect to Docker daemon"**

```bash
# On AWS instance, add ec2-user to docker group (already done) and restart session
# Or use sudo for Docker commands temporarily
```

**3. Services not starting**

```bash
# On AWS, give services 1-2 minutes to start, then check:
curl http://51.21.198.139:8761  # Eureka should respond
curl http://51.21.198.139:8080/actuator/health  # API Gateway health
```

**4. Frontend can't reach backend**

- Verify all ports (4200-4201, 8080-8083, 8761) are open in AWS Security Group
- Check browser console for CORS errors
- Verify API Gateway is responding

## 📊 What Happens During Deployment

1. **Jenkins triggers** on git push
2. **Backend builds** with Maven
3. **Backend tests** with JUnit
4. **Frontend builds** with Angular
5. **Frontend tests** with Jasmine/Karma
6. **Deploy stage**:
   - Builds Docker images locally
   - Saves images as tar files
   - Transfers to AWS instance (51.21.198.139)
   - Loads images on AWS
   - Stops old containers
   - Starts new containers
   - Performs health check
7. **Email notification** sent on success/failure

## 🎯 Next Deploy

Every time you push to `main` branch, Jenkins will automatically:

1. Run all tests
2. If tests pass → Deploy to AWS
3. If tests fail → Send failure email, no deployment

## 📧 Email Notifications

You'll receive emails at:

- `othmane.afilali@gritlab.ax`
- `jedi.reston@gritlab.ax`

For:

- ✅ Successful deployments
- ❌ Failed builds/tests
- ⚠️ Unstable builds

## 💡 Tips

- First deployment takes 5-10 minutes (Docker image transfer)
- Subsequent deployments take 3-5 minutes
- Services take 1-2 minutes to fully start after containers are up
- Monitor Jenkins console output for real-time deployment status
- Check AWS instance logs if issues occur

---

**Need detailed setup info?** See [AWS_DEPLOYMENT_SETUP.md](AWS_DEPLOYMENT_SETUP.md)
