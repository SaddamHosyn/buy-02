# buy-02!

**Full-stack e-commerce marketplace — shopping cart, orders, analytics dashboards, search & filtering — with CI/CD and SonarQube quality gates.**

![Java](https://img.shields.io/badge/Java-50%25-orange)
![Docker](https://img.shields.io/badge/Docker-25%25-blue)
![NoSQL](https://img.shields.io/badge/NoSQL-30%25-green)
![Backend](https://img.shields.io/badge/Backend-60%25-yellow)
![Frontend](https://img.shields.io/badge/Frontend-37%25-purple)

---

## 📋 Overview

This project finalizes a full e-commerce web application by adding core features: **shopping cart**, **orders**, **user & seller profiles with analytics**, and **product search/filtering**.

It integrates work from previous projects (auth, product/media, microservices) and enforces best practices: **PR workflows**, **CI/CD (Jenkins)**, and **code quality (SonarQube)**.

**Your role**: Full-stack engineer wrapping up the marketplace for launch.
**Your mission**: Deliver a secure, performant, and user-friendly experience from **browsing → cart → checkout → order tracking**, with clean code, CI/CD, and clear documentation.

## 🎯 Learning Objectives

- Design and extend data models for carts, orders, and profiles
- Implement RESTful APIs for carts, orders, search, and analytics
- Build Angular views (cart, checkout, order history, dashboards)
- Add search & filtering (text + facets) with efficient querying
- Apply error handling, validation, and role-based security
- Practice team workflows (PRs, reviews) and CI/CD with Jenkins
- Measure and improve code quality using SonarQube
- Write tests (unit, integration, e2e) for critical flows

## 🎯 Quick Start

1. **Start the CI/CD pipeline:** `cd .pipeline && ./boot-pipeline.sh`
2. **Push code** → Automatic build, test, quality gate & deploy via webhook
3. **Access app locally**: http://localhost:4200
4. **Jenkins**: http://localhost:8088 · **SonarQube**: http://localhost:9000

> See [.pipeline/README.md](.pipeline/README.md) for full CI/CD setup instructions.

## ✅ Implemented Features

### 🛒 Shopping Cart & Checkout

- Add/update/remove items in cart
- Cart persistence across page refreshes
- Checkout wizard: address → review → confirm
- "Pay on Delivery" payment method
- Order creation from cart contents

### 📦 Orders Management

- Order list with search (status, date)
- Order details with status timeline and totals
- Cancel, redo, and remove orders
- Seller view of related orders
- Order status tracking (PENDING → CONFIRMED → SHIPPED → DELIVERED)

### 👤 User & Seller Profiles with Analytics

- **User dashboard**: total spent, most-bought products, top categories (charts)
- **Seller dashboard**: revenue, best-selling products, units sold (charts)
- Profile management with avatar upload

### 🔎 Search & Filtering

- Product search with keyword bar
- Filters: category, price sliders, sort options
- Pagination for large result sets
- Efficient MongoDB querying

### 🔐 Security & Error Handling

- JWT authentication with role-based access (SELLER, CLIENT, ADMIN)
- Ownership enforcement on mutations
- Global exception handlers with consistent error schema `{ code, message, details }`
- Inline form errors + snackbar notifications for API failures
- HTTPS enforcement, secure CORS at gateway

### ⚙️ CI/CD Pipeline

- **Jenkins**: build → test → SonarQube quality gate → package → deploy
- **SonarQube**: code quality analysis for both backend and frontend
- **GitHub Webhook**: automatic builds on push
- **Docker Compose**: containerized deployment
- **Branch protection**: PRs required, green pipeline before merge

## 🔧 Jenkins Setup & Configuration

### Prerequisites

- **Jenkins** (LTS version) running and accessible
- **GitHub Account** with access to your repository
- **SMTP Server** configured for email notifications (e.g., Gmail with app password)
- **Docker** and **docker-compose** for deployment fallback
- **Maven 3.6+** and **Node.js 18+** for builds

### Step 1: Configure GitHub Credentials in Jenkins

1. Go to **Manage Jenkins** → **Manage Credentials**
2. Click **System** → **Global credentials** → **Add Credentials**
3. Create a **Username with password** credential:
   - **Username**: Your GitHub username
   - **Password**: [GitHub Personal Access Token](https://github.com/settings/tokens)
     - Scopes needed: `repo` and `admin:repo_hook`
   - **ID**: `github-credentials`
4. **Create** and verify connection

### Step 2: Set Up GitHub Webhook for Automatic Builds

1. In your GitHub repository → **Settings** → **Webhooks** → **Add webhook**
2. Configure:
   - **Payload URL**: `http://your-jenkins-url:8090/github-webhook/` (with trailing slash)
   - **Content type**: `application/json`
   - **Events**: Select "Just the push event"
   - **Active**: ✓ Check this box
3. **Add webhook**

**For localhost Jenkins**, use [ngrok](https://ngrok.com/) to expose Jenkins:

```bash
brew install ngrok
ngrok http 8088
# Use the https:// URL provided as your Payload URL
```

### Step 3: Configure Email Notifications

1. **Manage Jenkins** → **Configure System**
2. Scroll to **E-mail Notification** section:
   - **SMTP server**: `smtp.gmail.com`
   - **SMTP port**: `587`
   - **Default user e-mail suffix**: `@gmail.com`
3. Check ☑️ **Use SMTP Authentication**
   - **Username**: Your Gmail address
   - **Password**: [Gmail App Password](https://support.google.com/accounts/answer/185833)
   - **Use TLS**: ✓ Check this box
4. **Test configuration** and **Save**

### Step 4: Configure Jenkins Job

1. Create a new **Pipeline** job (or copy an existing one)
2. **Pipeline** section → **Definition**: Select "Pipeline script from SCM"
3. **SCM**: Select **Git**
   - **Repository URL**: `https://github.com/SaddamHosyn/buy-02.git`
   - **Credentials**: Select the GitHub credentials you created
   - **Branch**: `*/feature` (or your working branch)
4. **Script Path**: `.pipeline/Jenkinsfile`
5. Under **Build Triggers** → Check ☑️ **GitHub hook trigger for GITScm polling**
6. **Save**

### Step 5: Trigger Your First Build

Simply push code to trigger an automatic build:

```bash
git add .
git commit -m "Test Jenkins webhook"
git push origin cleanup
```

Jenkins will automatically start a build within a few seconds!

### Accessing Build Reports

After a successful build:

1. **Jenkins UI** → Your job → Build #N
2. **Test Results**: Shows parsed JUnit test results
3. **Artifacts**: Download archived test reports and coverage files
4. **Email Notification**: Check your inbox for HTML report with direct links

## 📊 Pipeline Structure

The `Jenkinsfile` defines a multi-stage pipeline with the following flow:

### Pipeline Stages

```
1. Validate Environment
   ├─ Check Maven, Node.js, npm, Docker, docker-compose, Git, Chrome
   └─ Fail if required tools missing

2. Build
   └─ mvn clean install -DskipTests

3. Test (Parallel)
   ├─ Backend Tests (mvn test + JUnit reports)
   └─ Frontend Tests (Karma/Jasmine + coverage)

4. SonarQube Analysis (Parallel, main branch only)
   ├─ Backend Analysis (Maven sonar:sonar)
   ├─ Frontend Analysis (sonarqube-scanner)
   └─ Quality Gate check (waitForQualityGate)

5. Deploy (main branch only)
   ├─ Build Docker images for all services
   └─ docker compose up (containerized deployment)

6. Post Actions
   ├─ Archive test reports
   ├─ Success / Failure notifications
   └─ Cleanup
```

### Environment Variables

The pipeline uses these key variables:

| Variable             | Purpose                         |
| -------------------- | ------------------------------- |
| `JENKINS_SCRIPTS`    | Path to Jenkins helper scripts  |
| `SONARQUBE_URL`      | SonarQube server URL            |
| `MONGO_ROOT_USERNAME`| MongoDB credentials (Jenkins)   |
| `MONGO_ROOT_PASSWORD`| MongoDB credentials (Jenkins)   |
| `API_GATEWAY_URL`    | API Gateway URL for services    |

### Test Report Files

After build completion, you'll find:

- **Backend Tests**: `**/target/surefire-reports/*.xml`
- **Frontend Tests**: `buy-01-ui/coverage/` (if coverage is enabled)
- **Build Artifacts**: Accessible in Jenkins UI under "Artifacts"

## 🏗️ Architecture Overview

This project implements a modern microservices architecture with the following components:

### Backend Services (Spring Boot 3.5.6 + Java 17)

- **API Gateway** (Port 8080) - HTTP entry point with routing and CORS configuration
- **Service Registry** (Port 8761) - Eureka service discovery for dynamic service registration
- **User Service** (Port 8081) - User authentication, JWT management, and profile handling
- **Product Service** (Port 8082) - Product catalog, inventory, and seller management
- **Media Service** (Port 8083) - File uploads, media storage, and image management
- **Order Service** (Port 8084) - Order and cart management

### Frontend

- **Angular 20** (Ports 4201) - Modern SPA with Angular Material Design
  - HTTPS on port 4201 (with self-signed certificates)

### Infrastructure

- **Apache Kafka** - Event-driven messaging for cascade operations and data consistency
- **Zookeeper** - Kafka coordination and cluster management
- **MongoDB 6.0** - NoSQL database with database-per-service pattern

## ✨ Key Features

### Authentication & Authorization

- 🔐 **JWT Authentication** with secure token-based auth
- 👥 **Role-Based Access Control** (SELLER, CLIENT, ADMIN)
- 🔑 **Password Management** with secure hashing
- 👤 **User Profiles** with avatar upload and management

### Architecture & Scalability

- 📨 **Event-Driven Architecture** using Kafka for cascade operations
- 🎯 **Service Discovery** with Eureka for dynamic load balancing
- 🗄️ **Database per Service** pattern for data isolation
- 🐳 **Fully Dockerized** - one command deployment
- 🔄 **CORS Configuration** for cross-origin requests
- ♻️ **Clean Code** - refactored with DRY principles and helper methods for maintainability

### Product & Media Management

- 📦 **Product CRUD** with seller dashboard
- 📁 **Multi-File Upload** with validation (images, documents)
- 🖼️ **Image Management** with preview and lightbox
- 📊 **Media Analytics** and tracking

### User Experience & High-End Tech

- 🎨 **Modern Material UI** with responsive design (Mobile & Desktop)
- ⚡ **Reactive Forms** with multi-level validation & custom rules
- 🛡️ **Functional Route Guards** for robust role-based security
- 📡 **HTTP Interceptor Pipeline** for JWT management & global error handling
- 🔔 **Intelligent Notification System** with Material Snackbars
- � **Dynamic Analytics Dashboards** for both Buyers and Sellers (Chart.js)
- 🔍 **Advanced Search Engine** with regex partial matching & dynamic filters
- 🌓 **Dark/Light Theme** support (Material theming)

## 🚀 Quick Start

### Prerequisites

- **Docker** - Required for containerized deployment
- **Java 17+** - For local development (optional)
- **Node.js 18+** and npm - For frontend development (optional)
- **Maven 3.6+** - For building services locally (optional)

### One-Command Deployment (Recommended)

The easiest way to run the entire application:

```bash
# Clone the repository
git clone https://github.com/SaddamHosyn/buy-02.git
cd buy-02

# Or use the provided helper script
./start_docker.sh

# Check services status
docker-compose ps
```

**Helper Scripts Available:**

- `./start_all.sh` - Builds and starts all services (Docker + local builds)
- `./stop_all.sh` - Stops all running services
- `./start_docker.sh` - Starts only Docker infrastructure (Kafka, MongoDB, Zookeeper)
- `./shutdown_all.sh` - Gracefully shuts down all containers

**Access the application:**

- 🔒 **Frontend (HTTPS)**: https://localhost:4201 (self-signed certificate)
- 🔌 **API Gateway**: http://localhost:8080
- 📊 **Eureka Dashboard**: http://localhost:8761
- 🗄️ **MongoDB**: mongodb://localhost:27017

### Local Development Setup

For development with hot-reload:

1. **Start infrastructure only:**

```bash
docker-compose up -d zookeeper kafka mongodb
```

2. **Run backend services:**

```bash
# Build all services
mvn clean install

# Start services (each in separate terminal)
cd service-registry && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
cd user-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd media-service && mvn spring-boot:run
```

3. **Run frontend with hot-reload:**

```bash
cd buy-01-ui
npm install
npm start
```

### First Time Setup

After starting the application, you can:

1. **Register a new account:**
   - Navigate to http://localhost:4200
   - Click "Register" and create an account
   - Choose role: SELLER (to sell products) or CLIENT (to buy products)

2. **Verify services:**
   - Check Eureka dashboard: http://localhost:8761
   - All services should show as "UP"

3. **Start using the platform:**
   - **Sellers**: Upload products, manage inventory, upload media
   - **Clients**: Browse products, view details, manage profile

## 📊 Service Ports & URLs

### Local Development (via `start_all.sh`)

| Service          | Port  | Protocol | URL                       | Description           |
| ---------------- | ----- | -------- | ------------------------- | --------------------- |
| Frontend (HTTPS) | 4201  | HTTPS    | https://localhost:4201    | Secure frontend       |
| API Gateway      | 8080  | HTTP     | http://localhost:8080     | Main API entry point  |
| Service Registry | 8761  | HTTP     | http://localhost:8761     | Eureka dashboard      |
| User Service     | 8081  | HTTP     | Internal                  | User management       |
| Product Service  | 8082  | HTTP     | Internal                  | Product management    |
| Media Service    | 8083  | HTTP     | Internal                  | Media/file management |
| Order Service    | 8084  | HTTP     | Internal                  | Order management      |
| MongoDB          | 27017 | TCP      | mongodb://localhost:27017 | Database server       |
| Kafka            | 9092  | TCP      | localhost:9092            | Message broker        |
| Zookeeper        | 2182  | TCP      | localhost:2182            | Kafka coordination    |

### Pipeline Deployment (via Jenkins / `docker compose`)

Pipeline host ports are offset to allow local dev and pipeline to coexist:

| Service          | Port  | Protocol | URL                       | Description           |
| ---------------- | ----- | -------- | ------------------------- | --------------------- |
| Frontend (HTTPS) | 4201  | HTTPS    | https://localhost:4201    | Secure frontend       |
| API Gateway HTTP | 8090  | HTTP     | http://localhost:8090     | Pipeline API entry    |
| API Gateway HTTPS| 8444  | HTTPS    | https://localhost:8444    | Pipeline API (HTTPS)  |
| Service Registry | 8762  | HTTP     | http://localhost:8762     | Eureka dashboard      |
| User Service     | 8091  | HTTP     | Internal                  | User management       |
| Product Service  | 8092  | HTTP     | Internal                  | Product management    |
| Media Service    | 8093  | HTTP     | Internal                  | Media/file management |
| Order Service    | 8094  | HTTP     | Internal                  | Order management      |
| MongoDB          | 27018 | TCP      | mongodb://localhost:27018 | Database server       |
| Kafka            | 9094  | TCP      | localhost:9094            | Message broker        |
| Zookeeper        | 2182  | TCP      | localhost:2182            | Kafka coordination    |

**Note:** Internal services communicate through the API Gateway and Docker network. The offset ports only affect host-side access.

## 🔄 Event-Driven Flow

The system uses Kafka for cascade deletion operations and data consistency:

```
User Deletion → Kafka Topic: user.deleted → Product Service & Media Service
                                          ↓                    ↓
                              Delete User's Products    Delete User's Media
                                          ↓
                              Kafka Topic: product.deleted
                                          ↓
                                     Media Service
                                          ↓
                              Delete Product Media Files
```

**Key Points:**

- When a user is deleted, both product and media services receive the event
- Product service deletes all products owned by that user and publishes `product.deleted` events
- Media service receives `product.deleted` events and cleans up associated media files
- Media service also directly handles user deletions to remove orphaned media
- All file deletions are handled by a centralized helper method to avoid code duplication

## Kafka & MongoDB

### Kafka Overview

- **Producers:** `user-service` publishes `user.deleted` (minimal payload, typically the user id). `product-service` deletes products and publishes `product.deleted` events (preferred payload is JSON with `id` and `mediaIds`).
- **Consumers:** `product-service` listens for `user.deleted` and deletes the user's products. `media-service` listens for `product.deleted` and deletes media. `media-service` also listens for `user.deleted` as a fallback to remove user-owned media.
- **Topics:** `user.deleted`, `product.deleted` (created by each service via `KafkaTopicConfig` beans).
- **Message formats:** prefer small, typed JSON events like `{ "id": "<productId>", "mediaIds": ["<mediaId>", ...] }`. Consumers also accept older plain-string messages containing the product id.

**Kafka - Docker commands (list topics, consume, produce)**

- **List all topics:**

```bash
docker exec -it buy-01-kafka-1 /bin/bash -c \
  "/usr/bin/kafka-topics --bootstrap-server localhost:9092 --list"
```

- **Consume messages from a topic (show headers):**

```bash
docker exec -it buy-01-kafka-1 /bin/bash -c \
  "/usr/bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic product.deleted --from-beginning --property print.headers=true"
```

- **Produce a JSON message to a topic (useful for tests):**

```bash
echo '{"id":"69246f37ee23ecd66ed8ca65","mediaIds":["69246f370ca32276270f8123"]}' \
  | docker exec -i buy-01-kafka-1 /usr/bin/kafka-console-producer --bootstrap-server localhost:9092 --topic product.deleted
```

- **Produce a plain product id (legacy):**

```bash
echo "69246f37ee23ecd66ed8ca65" | docker exec -i buy-01-kafka-1 /usr/bin/kafka-console-producer --bootstrap-server localhost:9092 --topic product.deleted
```

### MongoDB

- **Connect using `mongosh` from your host (local port mapping):**

```bash
mongosh "mongodb://root:example@localhost:27017/?authSource=admin"
```

- **Or exec into the MongoDB container and launch `mongosh`:**

```bash
docker exec -it buy-01-mongodb-1 mongosh -u root -p example --authenticationDatabase admin
```

- **Inspect media DB and collection (example):**

```bash
// list databases
show dbs

// switch to the product DB
use productdb

// list collections
show collections

// show a few documents
db.products.find().limit(5).pretty()

// find media by userId
db.media.find({ userId: "69244af654df39660cbd3294" }).pretty()

// delete media by ObjectId (if _id is an ObjectId)
db.media.deleteOne({ _id: ObjectId("69246f370ca32276270f8123") })

// Count all documents in the collection
db.media.countDocuments({})

// Count documents matching a filter (e.g. media owned by a user)
db.media.countDocuments({ userId: "69244af654df39660cbd3294" })
```

## 🗂️ Project Structure

```
buy-02/
├── api-gateway/              # API Gateway with routing and CORS
│   ├── src/main/
│   │   ├── java/.../apigateway/
│   │   │   ├── ApiGatewayApplication.java
│   │   │   └── config/
│   │   │       └── CorsConfig.java       # CORS configuration
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-docker.yml
│   │       └── application.yml           # Route definitions (Port 8080)
│   ├── Dockerfile
│   └── pom.xml
│
├── service-registry/         # Eureka server for service discovery
│   ├── src/main/
│   │   ├── java/.../serviceregistry/
│   │   │   └── ServiceRegistryApplication.java
│   │   └── resources/
│   │       └── application.properties
│   ├── Dockerfile
│   └── pom.xml
│
├── user-service/             # User management & authentication
│   ├── src/main/
│   │   ├── java/.../user/
│   │   │   ├── UserServiceApplication.java
│   │   │   ├── config/          # JWT, Security, Kafka
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── model/           # User, Role entities
│   │   │   ├── repository/      # MongoDB repositories
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       └── application.properties
│   ├── Dockerfile
│   └── pom.xml
│
├── product-service/          # Product catalog management
│   ├── src/main/
│   │   ├── java/.../product/
│   │   │   ├── ProductServiceApplication.java
│   │   │   ├── config/          # Security, Kafka, MongoDB
│   │   │   ├── controller/      # Product REST API
│   │   │   ├── dto/             # Request/Response DTOs
│   │   │   ├── model/           # Product entity
│   │   │   ├── repository/      # Product repository
│   │   │   └── service/         # Product logic, Kafka consumer
│   │   └── resources/
│   │       └── application.properties
│   ├── Dockerfile
│   └── pom.xml
│
├── media-service/            # Media file management
│   ├── src/main/
│   │   ├── java/.../media/
│   │   │   ├── MediaServiceApplication.java
│   │   │   ├── config/          # Storage, Security, Kafka
│   │   │   ├── controller/      # Media upload/download
│   │   │   ├── model/           # Media entity
│   │   │   ├── repository/      # Media repository
│   │   │   └── service/         # File handling, Kafka consumers
│   │   └── resources/
│   │       └── application.properties
│   ├── uploads/             # Local file storage
│   ├── Dockerfile
│   └── pom.xml
│
├── order-service/            # Order & cart management
│   ├── src/main/
│   │   ├── java/.../order/
│   │   │   ├── OrderServiceApplication.java
│   │   │   ├── config/          # Security, Kafka, MongoDB
│   │   │   ├── controller/      # Order & Cart REST API
│   │   │   ├── dto/             # Request/Response DTOs
│   │   │   ├── model/           # Order, Cart, OrderItem entities
│   │   │   ├── repository/      # Order & Cart repositories
│   │   │   └── service/         # Order logic, Kafka consumer
│   │   └── resources/
│   │       └── application.properties
│   ├── Dockerfile
│   └── pom.xml
│
├── buy-01-ui/               # Angular 20 frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/            # Core services & infrastructure
│   │   │   │   ├── guards/      # Auth & role guards
│   │   │   │   ├── interceptors/ # HTTP interceptors
│   │   │   │   ├── services/    # Auth, Product, Media services
│   │   │   │   └── validators/  # Custom validators
│   │   │   ├── features/        # Feature modules
│   │   │   │   ├── auth/        # Login, Register
│   │   │   │   ├── cart/         # Shopping cart & checkout
│   │   │   │   ├── orders/      # Order list, details, tracking
│   │   │   │   ├── products/    # Product list, detail, search
│   │   │   │   ├── profile/     # User profile & analytics
│   │   │   │   └── seller/      # Seller dashboard & analytics
│   │   │   └── shared/          # Shared components
│   │   │       ├── components/  # Reusable UI components
│   │   │       └── services/    # Shared services
│   │   ├── environments/        # Environment configs
│   │   │   ├── environment.ts
│   │   │   └── environment.prod.ts
│   │   ├── index.html
│   │   ├── main.ts
│   │   └── styles.css
│   ├── certs/                   # SSL certificates for HTTPS
│   │   ├── localhost.pem
│   │   └── localhost-key.pem
│   ├── nginx-https.conf         # Nginx config for HTTPS
│   ├── Dockerfile
│   ├── angular.json
│   ├── package.json
│   └── tsconfig.json
│
├── .pipeline/                   # CI/CD infrastructure
│   ├── Jenkinsfile              # Pipeline definition
│   ├── docker-compose.yml       # Pipeline container orchestration
│   ├── boot-pipeline.sh         # Start Jenkins + SonarQube
│   └── pipeline-tools.sh        # Troubleshooting utilities
│
├── pom.xml                      # Maven parent POM (aggregator)
└── README.md                    # This file
```

## 🛠️ Technologies Used

### Backend

- Spring Boot 3.5.6
- Spring Cloud (Eureka, Gateway)
- Spring Security with JWT
- Spring Data MongoDB
- Spring Kafka
- Maven
- Lombok (for cleaner code with annotations)

### Frontend

- Angular 20
- Angular Material
- RxJS
- TypeScript 5.9

### Infrastructure

- Apache Kafka
- MongoDB 6.0
- Docker & Docker Compose
- Nginx (for HTTPS frontend)

### Code Quality

- DRY Principles (Don't Repeat Yourself)
- Helper methods for common operations
- Consistent error handling
- Clean architecture patterns

## 🔐 Security Features

- **JWT Tokens**: Stateless authentication with secure token generation and validation
- **Role-Based Authorization**: Fine-grained access control (SELLER, CLIENT, ADMIN)
- **Password Encryption**: Bcrypt hashing for secure password storage
- **CORS Configuration**: Properly configured to allow frontend-backend communication
- **Frontend HTTPS**: Optional HTTPS support with self-signed certificates (port 4201)
- **Input Validation**: Server-side and client-side validation for all inputs
- **File Upload Security**: File type and size validation, secure storage
- **JWT Secret**: Configurable secret key for token signing

## 🧪 Testing

**Backend Tests:**

```bash
mvn test
```

**Frontend Tests:**

```bash
cd buy-01-ui
npm test
```

## 📝 Environment Variables

Key environment variables (configured in `docker-compose.yml`):

```yaml
SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
SPRING_DATA_MONGODB_URI: mongodb://root:example@mongodb:27017/{dbname}?authSource=admin
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://service-registry:8761/eureka/
```

## 🐛 Troubleshooting

### Services Not Starting

**Check all containers:**

```bash
docker-compose ps
docker-compose logs
```

**Restart specific service:**

```bash
docker-compose restart <service-name>
# Example: docker-compose restart api-gateway
```

**Rebuild after code changes:**

```bash
docker-compose build --no-cache <service-name>
docker-compose up -d <service-name>
```

### Kafka Issues

**Services can't connect to Kafka:**

```bash
# Check Kafka is running
docker ps | grep kafka

# View Kafka logs
docker-compose logs kafka

# List Kafka topics
docker exec -it buy-01-kafka-1 /bin/bash -c \
  "/usr/bin/kafka-topics --bootstrap-server localhost:9092 --list"
```

### Database Issues

**MongoDB connection failed:**

```bash
# Check MongoDB is running
docker ps | grep mongodb

# Test connection
docker exec -it buy-01-mongodb-1 mongosh -u root -p example

# View MongoDB logs
docker-compose logs mongodb
```

**Clear MongoDB data:**

```bash
# Stop services
docker-compose down

# Remove data volume (WARNING: This deletes all data!)
rm -rf ./uploads

# Restart
docker-compose up -d
```

### Frontend Issues

**Frontend can't reach backend:**

- Ensure API Gateway is running: http://localhost:8080
- Check browser console for CORS errors
- Verify environment configuration in `buy-01-ui/src/environments/`

**Mixed Content warnings (HTTPS frontend calling HTTP backend):**

- This is normal when using HTTPS frontend (port 4201)
- Use HTTP frontend (port 4200) for development
- Browser auto-upgrades requests, which is safe

### Service Discovery Issues

**Services not registering with Eureka:**

- Wait 30-60 seconds after startup for registration
- Check Eureka dashboard: http://localhost:8761
- Verify service logs: `docker-compose logs <service-name>`

### Port Conflicts

**Port already in use:**

```bash
# Find process using port (Windows)
netstat -ano | findstr :8090

# Find process using port (Mac/Linux)
lsof -i :8090

# Kill process or change port in docker-compose.yml
```

## 👥 User Management

### Registering Users

**Via Frontend (Recommended):**

1. Navigate to http://localhost:4200
2. Click "Register"
3. Fill in the form (name, email, password, role)
4. Choose role: SELLER or CLIENT

**Via API:**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Seller",
    "email": "seller@example.com",
    "password": "password123",
    "role": "SELLER"
  }'
```

### User Roles

- **SELLER**: Create/edit/delete products, upload media, manage inventory, view orders & analytics
- **CLIENT**: Browse & search products, manage cart, place orders, track order status, view spending analytics
- **ADMIN**: Full system access (future implementation)

## 📚 API Documentation

All API endpoints are accessed through the API Gateway: `http://localhost:8080`

### Authentication Endpoints

| Method | Endpoint                    | Description             | Auth Required |
| ------ | --------------------------- | ----------------------- | ------------- |
| POST   | `/api/auth/register`        | Register new user       | No            |
| POST   | `/api/auth/login`           | Login and get JWT token | No            |
| POST   | `/api/auth/change-password` | Change user password    | Yes           |

### User Endpoints

| Method | Endpoint                    | Description              | Auth Required | Role      |
| ------ | --------------------------- | ------------------------ | ------------- | --------- |
| GET    | `/api/users/profile`        | Get current user profile | Yes           | Any       |
| PUT    | `/api/users/profile`        | Update user profile      | Yes           | Any       |
| PUT    | `/api/users/profile/name`   | Update user name         | Yes           | Any       |
| POST   | `/api/users/profile/avatar` | Upload user avatar       | Yes           | Any       |
| DELETE | `/api/users/{id}`           | Delete user (cascade)    | Yes           | Own/Admin |

### Product Endpoints

| Method | Endpoint                        | Description              | Auth Required | Role           |
| ------ | ------------------------------- | ------------------------ | ------------- | -------------- |
| GET    | `/api/products`                 | Get all products         | No            | Any            |
| GET    | `/api/products/{id}`            | Get product by ID        | No            | Any            |
| GET    | `/api/products/seller/{userId}` | Get products by seller   | No            | Any            |
| POST   | `/api/products`                 | Create new product       | Yes           | SELLER         |
| PUT    | `/api/products/{id}`            | Update product           | Yes           | SELLER (owner) |
| DELETE | `/api/products/{id}`            | Delete product (cascade) | Yes           | SELLER (owner) |

### Media Endpoints

| Method | Endpoint                                     | Description                  | Auth Required | Role           |
| ------ | -------------------------------------------- | ---------------------------- | ------------- | -------------- |
| POST   | `/api/media/upload`                          | Upload media file            | Yes           | SELLER         |
| POST   | `/api/media/upload-multiple`                 | Upload multiple files        | Yes           | SELLER         |
| GET    | `/api/media/{id}`                            | Get media by ID              | No            | Any            |
| GET    | `/api/media/user/{userId}`                   | Get user's media             | Yes           | Own/SELLER     |
| GET    | `/api/media/download/{filename}`             | Download file                | No            | Any            |
| DELETE | `/api/media/{id}`                            | Delete media                 | Yes           | SELLER (owner) |
| POST   | `/api/media/{mediaId}/associate/{productId}` | Associate media with product | Yes           | SELLER         |

### Request Examples

**Login:**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "seller@example.com",
    "password": "password123"
  }'
```

**Create Product (requires JWT token):**

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "Product Name",
    "description": "Product description",
    "price": 99.99,
    "category": "Electronics",
    "stock": 10
  }'
```

**Upload Media:**

```bash
curl -X POST http://localhost:8080/api/media/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/image.jpg"
```

## 🎯 Use Cases

### For Clients

1. **Register as CLIENT** → Browse marketplace
2. **Search & Filter** → Find products by keyword, category, price range
3. **Add to Cart** → Build shopping cart with quantity controls
4. **Checkout** → Complete purchase with "Pay on Delivery"
5. **Track Orders** → View order status timeline
6. **View Dashboard** → Total spent, most-bought products, top categories
7. **Manage Profile** → Update info and avatar

### For Sellers

1. **Register as SELLER** → Access seller dashboard
2. **Create Products** → List items with details, pricing, and images
3. **Manage Inventory** → Edit or delete products
4. **View Orders** → See customer orders for your products
5. **Manage Orders** → Update order status (confirm, ship, deliver)
6. **View Analytics** → Revenue, best-selling products, units sold
7. **Upload Media** → Add product images and files

### System Features

- **Cascade Deletion**: Deleting a user removes their products, media, and orders
- **Event-Driven**: Kafka ensures data consistency across services
- **Service Discovery**: Eureka enables dynamic service registration and load balancing

## 📋 Project Constraints

- Must implement: **Cart**, **Orders**, **Profiles (analytics)**, **Search/Filter**, **Pay on Delivery**
- Public endpoints are read-only (e.g., product search); all mutations require auth
- Pipeline must include tests + SonarQube quality gate
- Feature branches required; PRs for every change
- Code reviews (security, performance, readability) before merge
- Main branch protected: require approved reviews and green pipeline

## 📝 Audit Checklist

### Functional

- [ ] Database design correctly implemented with new relationships
- [ ] PRs and code reviews followed throughout development
- [ ] Orders MicroService, User Profile, Search/Filtering, Shopping Cart implemented
- [ ] Cart items persist across page refresh with correct quantities
- [ ] SonarQube issues identified and addressed
- [ ] UI is user-friendly and responsive
- [ ] Error handling and validation in place with appropriate messages
- [ ] Security measures consistently applied

### Collaboration & Process

- [ ] Code reviews performed for each PR
- [ ] CI/CD pipeline (Jenkins) correctly set up and used for PRs
- [ ] Branches merged correctly, main codebase up-to-date
- [ ] Application passes comprehensive testing of all new features
- [ ] Unit tests in place for critical parts of the application

### Bonus

- [ ] Wishlist feature (save for later)
- [ ] Additional payment methods (simulate gateway integration)

## 📖 Documentation

### For Developers

- **Backend**: Spring Boot REST APIs with Spring Security
- **Frontend**: Angular with reactive patterns and Material UI
- **Database**: MongoDB with database-per-service pattern
- **Messaging**: Kafka for event-driven architecture
- **Containerization**: Docker Compose for multi-container deployment
- **CI/CD**: Jenkins pipeline with SonarQube quality gates

### Key Design Patterns

- Microservices Architecture
- API Gateway Pattern
- Service Discovery Pattern
- Event-Driven Architecture
- Database per Service
- JWT Authentication

## 📄 License

This project is developed for educational purposes as part of a university project.

## 👨‍💻 Contributors

- [@jeeeeedi](https://github.com/jeeeeedi)
- [@oafilali](https://github.com/oafilali)
- [@ejmilli](https://github.com/ejmilli)
- [@SaddamHosyn](https://github.com/SaddamHosyn)

---

**Built with ❤️ using Spring Boot, Angular, Kafka, and MongoDB**

_For questions or issues, please open an issue on GitHub._
