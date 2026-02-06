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

## 🐳 Troubleshooting
- **Build failing on SonarQube?** Check that your `sonarqube-token` in Jenkins matches the one in SonarQube.
- **"UnknownHostException: sonarqube"?** Update Jenkins System Config -> SonarQube URL to `http://host.docker.internal:9000`.
- **"401 Unauthorized"?** Refresh the `sonarqube-token` credential in Jenkins.
