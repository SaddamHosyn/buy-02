# CI/CD Pipeline & Environment Setup

This directory contains the infrastructure code for the Jenkins CI/CD pipeline, including SonarQube analysis and local service orchestration.

## 🚀 Quick Start

1. **Start the Infrastructure**
   ```bash
   cd .pipeline
   ./boot-pipeline.sh
   # Use --cleanup to reset: ./boot-pipeline.sh --cleanup
   ```
   This starts Jenkins, SonarQube, MongoDB, Kafka, and other required services.

2. **Access the Tools**
   - **Jenkins:** [http://localhost:8088](http://localhost:8088)
   - **SonarQube:** [http://localhost:9000](http://localhost:9000) (Default: `admin` / `admin`)

---

## ⚙️ Configuration Help

### 1. SonarQube Setup
1. Log in to **SonarQube** ([http://localhost:9000](http://localhost:9000)).
2. Go to **User Icon > My Account > Security**.
3. Generate a token (name it 'Jenkins').
4. **Copy this token immediately!**

### 2. Jenkins Configuration
#### A. Add Credentials
Go to **Manage Jenkins > Credentials > System > Global credentials** and add:

| ID | Kind | Secret / Value | Description |
|----|------|----------------|-------------|
| `sonarqube-token` | Secret text | *(Paste Token from Step 1)* | SonarQube Auth Token |
| `mongo-root-username`| Secret text | `admin` | MongoDB Username |
| `mongo-root-password`| Secret text | `gritlab25` | MongoDB Password |
| `github-token` | Secret text | *(Your GitHub PAT)* | GitHub Access (if using webhooks) |

#### B. System Configuration (Critical for MacOS/Docker)
Go to **Manage Jenkins > System > SonarQube servers**:
- **Name:** `SonarQube`
- **Server URL:** `http://host.docker.internal:9000`
- **Server authentication token:** Select `SonarQube Token`

*Note: `host.docker.internal` is required for Jenkins (running in Docker) to reach SonarQube (also in Docker) on macOS.*

---

## 🛠️ Pipeline Details
The pipeline is defined in `Jenkinsfile` and covers:
1. **Validation:** Checks environment setup.
2. **Build:** Compiles Java backend (`mvn clean install`).
3. **Tests:** Runs JUnit (Backend) and Karma/Jasmine (Frontend).
4. **Analysis:** Runs SonarQube scanner (Community Edition).
   - *Note: Branch analysis is disabled for Community Edition.*

## � Pipeline-Deployed Service Ports

When the pipeline deploys via `docker compose`, host ports are **offset** from the local dev ports to avoid conflicts:

| Service          | Local Dev Port | Pipeline Port | Container Port |
|------------------|---------------|---------------|----------------|
| API Gateway HTTP | 8080          | **8090**      | 8080           |
| API Gateway HTTPS| 8443          | **8444**      | 8443           |
| Service Registry | 8761          | **8762**      | 8761           |
| User Service     | 8081          | **8091**      | 8081           |
| Product Service  | 8082          | **8092**      | 8082           |
| Media Service    | 8083          | **8093**      | 8083           |
| Order Service    | 8084          | **8094**      | 8084           |
| MongoDB          | 27017         | **27018**     | 27017          |
| Kafka            | 9092          | **9094**      | 9092           |
| Zookeeper        | 2181          | **2182**      | 2181           |
| Frontend HTTP    | 4200          | 4200          | 80             |
| Frontend HTTPS   | 4201          | 4201          | 443            |

> Internal container-to-container communication uses the original ports via the Docker network.

## 🔐 SSL Certificates

The pipeline requires a `buy01-certs` Docker volume for HTTPS. It is auto-created by `boot-pipeline.sh`, or you can set it up manually:

```bash
./setup_certs.sh          # Creates volume + generates self-signed certs
./setup_certs.sh --force  # Regenerates certs even if they exist
```

You can also run it via `pipeline-tools.sh setup-certs`.

---

## 🐳 Troubleshooting
- **"port already in use"?** Your local services (via `start_all.sh`) may be running. Either stop them (`./stop_all.sh`) or rely on the offset ports above.
- **"external volume buy01-certs not found"?** Run `./setup_certs.sh` or `.pipeline/pipeline-tools.sh setup-certs`.
- **Build failing on SonarQube?** Check that your `sonarqube-token` in Jenkins matches the one in SonarQube.
- **"UnknownHostException: sonarqube"?** Update Jenkins System Config -> SonarQube URL to `http://host.docker.internal:9000`.
- **"401 Unauthorized"?** Refresh the `sonarqube-token` credential in Jenkins.
