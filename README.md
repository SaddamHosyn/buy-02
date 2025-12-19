# Buy-01 E-Commerce Platform 🛒

A production-ready, full-stack microservices-based e-commerce platform built with **Spring Boot** and **Angular**, featuring event-driven architecture with **Apache Kafka**, service discovery, and containerized deployment.

## 🏗️ Architecture Overview

This project implements a modern microservices architecture with the following components:

### Backend Services (Spring Boot 3.5.6 + Java 17)

- **API Gateway** (Port 8080) - HTTP entry point with routing and CORS configuration
- **Service Registry** (Port 8761) - Eureka service discovery for dynamic service registration
- **User Service** (Port 8081) - User authentication, JWT management, and profile handling
- **Product Service** (Port 8082) - Product catalog, inventory, and seller management
- **Media Service** (Port 8083) - File uploads, media storage, and image management

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

### User Experience

- 🎨 **Modern Material UI** with responsive design
- ⚡ **Reactive Forms** with real-time validation
- 🔔 **Notification System** for user feedback
- 🛡️ **Client-Side Guards** for route protection
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
git clone https://github.com/jeeeeedi/buy-01.git
cd buy-01

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
- 🗄️ **MongoDB**: mongodb://root:example@localhost:27017

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

| Service          | Port  | Protocol | URL                       | Description           |
| ---------------- | ----- | -------- | ------------------------- | --------------------- |
| Frontend (HTTPS) | 4201  | HTTPS    | https://localhost:4201    | Secure frontend       |
| API Gateway      | 8080  | HTTP     | http://localhost:8080     | Main API entry point  |
| Service Registry | 8761  | HTTP     | http://localhost:8761     | Eureka dashboard      |
| User Service     | 8081  | HTTP     | Internal                  | User management       |
| Product Service  | 8082  | HTTP     | Internal                  | Product management    |
| Media Service    | 8083  | HTTP     | Internal                  | Media/file management |
| MongoDB          | 27017 | TCP      | mongodb://localhost:27017 | Database server       |
| Kafka            | 9092  | TCP      | localhost:9092            | Message broker        |
| Zookeeper        | 2182  | TCP      | localhost:2182            | Kafka coordination    |

**Note:** Internal services (User, Product, Media) communicate through the API Gateway and are not directly exposed.

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
buy-01/
├── api-gateway/              # API Gateway with routing and CORS
│   ├── src/main/
│   │   ├── java/.../apigateway/
│   │   │   ├── ApiGatewayApplication.java
│   │   │   └── config/
│   │   │       └── CorsConfig.java       # CORS configuration
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-docker.yml
│   │       └── application.yml           # Route definitions
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
│   │   │   │   ├── products/    # Product list, detail
│   │   │   │   ├── profile/     # User profile
│   │   │   │   └── seller/      # Seller dashboard
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
├── docker-compose.yml           # Multi-container orchestration
├── pom.xml                      # Maven parent POM
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
netstat -ano | findstr :8080

# Find process using port (Mac/Linux)
lsof -i :8080

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

- **SELLER**: Can create, edit, and delete products; upload media; manage inventory
- **CLIENT**: Can browse products, view details, manage profile
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

### For Sellers

1. **Register as SELLER** → Access seller dashboard
2. **Upload Media** → Add product images
3. **Create Products** → List items with details, pricing, and images
4. **Manage Inventory** → Edit or delete products
5. **View Analytics** → Track product performance

### For Clients

1. **Register as CLIENT** → Browse marketplace
2. **View Products** → Search and filter products
3. **Product Details** → View images, descriptions, pricing
4. **Manage Profile** → Update info and avatar

### System Features

- **Cascade Deletion**: Deleting a user automatically removes their products and associated media
- **Event-Driven**: Kafka ensures data consistency across services
- **Service Discovery**: Eureka enables dynamic service registration and load balancing

## 🚧 Future Enhancements

- 🛒 Shopping cart functionality
- 💳 Payment integration
- 📧 Email notifications
- 🔍 Advanced search and filtering
- ⭐ Product reviews and ratings
- 📊 Seller analytics dashboard
- 🌐 Multi-language support
- 📱 Mobile app (React Native)

## 📖 Documentation

### For Developers

- **Backend**: Spring Boot REST APIs with Spring Security
- **Frontend**: Angular with reactive patterns and Material UI
- **Database**: MongoDB with database-per-service pattern
- **Messaging**: Kafka for event-driven architecture
- **Containerization**: Docker Compose for multi-container deployment

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
- [@Anastasia](https://github.com/An1Su)
- [@SaddamHosyn](https://github.com/SaddamHosyn)

---

**Built with ❤️ using Spring Boot, Angular, Kafka, and MongoDB**

_For questions or issues, please open an issue on GitHub._
