pipeline {
    agent any
    
    parameters {
        choice(
            name: 'DEPLOYMENT_TARGET',
            choices: ['AWS', 'Local Docker', 'Both'],
            description: 'Choose deployment target'
        )
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip test execution (not recommended for production)'
        )
        booleanParam(
            name: 'SKIP_FRONTEND_BUILD',
            defaultValue: false,
            description: 'Skip frontend build (backend changes only)'
        )
        booleanParam(
            name: 'FORCE_REBUILD',
            defaultValue: false,
            description: 'Force clean rebuild (ignore cache)'
        )
        string(
            name: 'CUSTOM_TAG',
            defaultValue: '',
            description: 'Custom Docker tag (leave empty for build number)'
        )
    }
    
    triggers {
        githubPush()  // Trigger on GitHub webhook push
    }

    options {
        // Keep last 30 builds
        buildDiscarder(logRotator(numToKeepStr: '30'))
        // Prevent concurrent builds
        disableConcurrentBuilds()
    }

    environment {
        // Load configuration from external file
        MAVEN_HOME = '/opt/apache-maven-3.9.9'
        NODEJS_HOME = '/usr/bin'
        PATH = "${MAVEN_HOME}/bin:${NODEJS_HOME}:${env.PATH}"
        
        // Email Configuration - Use credentials for sensitive data
        TEAM_EMAIL = credentials('team-email')  // Store in Jenkins Credentials
        
        // AWS Configuration - Use credentials for sensitive data
        AWS_DEPLOY_HOST = credentials('aws-deploy-host')  // Store in Jenkins Credentials
        AWS_DEPLOY_USER = credentials('aws-deploy-user')  // Store in Jenkins Credentials
        // AWS_SSH_KEY is handled via withCredentials block for proper Secret File binding
        
        // MongoDB Credentials - Use credentials for sensitive data
        MONGO_ROOT_USERNAME = credentials('mongo-root-username')  // Store in Jenkins Credentials
        MONGO_ROOT_PASSWORD = credentials('mongo-root-password')  // Store in Jenkins Credentials
        
        // API Gateway URL
        API_GATEWAY_URL = "http://13.61.234.232:8080"
        
        // Docker Configuration
        DOCKER_IMAGE_PREFIX = 'buy01-pipeline'
        SERVICE_REGISTRY_IMAGE = 'buy01-pipeline-service-registry'
        API_GATEWAY_IMAGE = 'buy01-pipeline-api-gateway'
        USER_SERVICE_IMAGE = 'buy01-pipeline-user-service'
        PRODUCT_SERVICE_IMAGE = 'buy01-pipeline-product-service'
        MEDIA_SERVICE_IMAGE = 'buy01-pipeline-media-service'
        FRONTEND_IMAGE = 'buy01-pipeline-frontend'
        
        // Paths
        JENKINS_SCRIPTS = './jenkins'
        FRONTEND_DIR = 'buy-01-ui'
    }

    stages {
        stage('Validate Environment') {
            steps {
                echo 'Validating build environment...'
                sh '${JENKINS_SCRIPTS}/validate-environment.sh'
            }
        }
        
        stage('Checkout') {
            options {
                timeout(time: 10, unit: 'MINUTES')
            }
            steps {
                checkout scm
            }
        }
        stage('Build Backend') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            steps {
                echo 'Building backend services...'
                script {
                    if (params.FORCE_REBUILD) {
                        sh 'mvn clean install -DskipTests -U'  // Force update dependencies
                    } else {
                        sh 'mvn clean install -DskipTests'
                    }
                }
            }
        }
        
        stage('Run Tests') {
            when {
                expression { !params.SKIP_TESTS }
            }
            options {
                timeout(time: 45, unit: 'MINUTES')
            }
            parallel {
                stage('Backend Tests') {
                    steps {
                        echo 'Running JUnit tests...'
                        sh '''
                            set -e
                            mvn test
                            if [ $? -ne 0 ]; then
                                echo "❌ Backend tests FAILED! Pipeline will STOP here."
                                exit 1
                            fi
                        '''
                    }
                    post {
                        always {
                            junit '**/target/surefire-reports/*.xml'
                        }
                    }
                }
                
                stage('Build Frontend') {
                    when {
                        expression { !params.SKIP_FRONTEND_BUILD }
                    }
                    steps {
                        echo 'Building frontend...'
                        dir('buy-01-ui') {
                            sh 'npm install'
                            sh 'npm run build'
                        }
                    }
                }
            }
        }
        
        stage('Test Frontend') {
            options {
                timeout(time: 20, unit: 'MINUTES')
            }
            steps {
                echo 'Running frontend tests...'
                dir('buy-01-ui') {
                    sh '''
                        set -e
                        mkdir -p target/surefire-reports
                        
                        # Detect Chrome binary location
                        export CHROME_BIN=$(which chromium-browser || which google-chrome || which chrome || echo "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" 2>/dev/null || echo "")
                        
                        if [ -z "$CHROME_BIN" ] || [ ! -f "$CHROME_BIN" ]; then
                            echo "⚠️ Warning: Chrome not found, skipping frontend tests"
                            echo "To fix: Install Chrome/Chromium or set CHROME_BIN environment variable"
                            exit 0
                        fi
                        
                        echo "Using Chrome: $CHROME_BIN"
                        npm test -- --watch=false --browsers=ChromeHeadless
                        if [ $? -ne 0 ]; then
                            echo "❌ Frontend tests FAILED! Pipeline will STOP here."
                            exit 1
                        fi
                    '''
                }
            }
        }
        stage('Deploy') {
            options {
                timeout(time: 60, unit: 'MINUTES')
            }
            steps {
                echo 'Deploying application...'
                script {
                    sh '''
                        # Make scripts executable
                        chmod +x ${JENKINS_SCRIPTS}/*.sh
                        
                        # Pre-deployment cleanup
                        ${JENKINS_SCRIPTS}/pre-deployment-cleanup.sh
                        
                        # Build Docker images
                        ${JENKINS_SCRIPTS}/build-docker-images.sh ${BUILD_NUMBER}
                    '''
                    
                    // Try AWS deployment first
                    def awsDeploymentSuccessful = false
                    
                    // Use withCredentials to properly bind the SSH key file
                    withCredentials([file(credentialsId: 'aws-ssh-key-file', variable: 'AWS_SSH_KEY_FILE')]) {
                        try {
                            echo '=========================================='
                            echo '🚀 Attempting AWS Deployment...'
                            echo '=========================================='
                            sh '''
                                echo "Setting up SSH key..."
                                export AWS_SSH_KEY="${AWS_SSH_KEY_FILE}"
                                chmod 600 "${AWS_SSH_KEY}"
                                ls -la "${AWS_SSH_KEY}"
                                
                                ${JENKINS_SCRIPTS}/deploy.sh ${BUILD_NUMBER}
                                echo "✅ AWS Deployment successful"
                            '''
                            awsDeploymentSuccessful = true
                            
                            // Post-deployment cleanup on success
                            sh '${JENKINS_SCRIPTS}/post-deployment-cleanup.sh'
                        } catch (Exception e) {
                            echo "⚠️ AWS Deployment failed: ${e.message}"
                            echo "Reason: Typically SSH key not found or AWS credentials unavailable"
                            awsDeploymentSuccessful = false
                        }
                    }
                    
                    // If AWS deployment fails, pipeline fails (no local fallback on Jenkins server)
                    if (!awsDeploymentSuccessful) {
                        echo '=========================================='
                        echo '❌ AWS Deployment REQUIRED but failed!'
                        echo '=========================================='
                        echo 'Please check:'
                        echo '  1. All 5 Jenkins credentials are configured'
                        echo '  2. AWS EC2 server is accessible'
                        echo '  3. SSH key has correct permissions'
                        echo '=========================================='
                        
                        // Attempt rollback
                        try {
                            echo "Rolling back changes..."
                            sh '${JENKINS_SCRIPTS}/rollback.sh'
                        } catch (Exception rollbackError) {
                            echo "⚠️ Rollback also failed: ${rollbackError.message}"
                        }
                        
                        error("Deployment failed - AWS credentials or connectivity issue")
                    }
                }
            }
        }
    }

    post {
        always {
            echo '=========================================='
            echo 'Publishing test results...'
            echo '=========================================='
            
            // Parse backend JUnit reports
            junit(
                testResults: '**/target/surefire-reports/*.xml',
                allowEmptyResults: true
            )
            
            // Archive test reports
            archiveArtifacts(
                artifacts: '**/target/surefire-reports/**/*.xml',
                allowEmptyArchive: true,
                fingerprint: true
            )
            
            // Archive coverage reports if they exist
            archiveArtifacts(
                artifacts: '**/target/site/jacoco/**/*,buy-01-ui/coverage/**/*',
                allowEmptyArchive: true,
                fingerprint: true
            )
        }
        success {
            echo '=========================================='
            echo '✅ Pipeline completed successfully!'
            echo '=========================================='
            
            script {
                try {
                    def template = readFile("${JENKINS_SCRIPTS}/email-success.html")
                    def emailBody = template
                        .replace('${BUILD_URL}', env.BUILD_URL ?: '')
                        .replace('${BUILD_NUMBER}', env.BUILD_NUMBER ?: '')
                        .replace('${JOB_NAME}', env.JOB_NAME ?: '')
                        .replace('${BUILD_DURATION}', currentBuild.durationString ?: '')
                        .replace('${BUILD_TIMESTAMP}', new Date().format('yyyy-MM-dd HH:mm:ss'))
                        .replace('${GIT_BRANCH}', env.GIT_BRANCH ?: 'main')
                    mail(
                        subject: "✅ BUILD SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: emailBody,
                        to: "${TEAM_EMAIL}",
                        mimeType: 'text/html'
                    )
                } catch (Exception e) {
                    echo "⚠️ Email notification failed: ${e.message}"
                }
            }
        }
        failure {
            echo '=========================================='
            echo '❌ Pipeline failed! Immediate action required'
            echo '=========================================='
            
            script {
                try {
                    def template = readFile("${JENKINS_SCRIPTS}/email-failure.html")
                    def emailBody = template
                        .replace('${BUILD_URL}', env.BUILD_URL ?: '')
                        .replace('${BUILD_NUMBER}', env.BUILD_NUMBER ?: '')
                        .replace('${JOB_NAME}', env.JOB_NAME ?: '')
                        .replace('${BUILD_DURATION}', currentBuild.durationString ?: '')
                        .replace('${BUILD_TIMESTAMP}', new Date().format('yyyy-MM-dd HH:mm:ss'))
                        .replace('${GIT_BRANCH}', env.GIT_BRANCH ?: 'main')
                    mail(
                        subject: "❌ BUILD FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: emailBody,
                        to: "${TEAM_EMAIL}",
                        mimeType: 'text/html'
                    )
                } catch (Exception e) {
                    echo "⚠️ Email notification failed: ${e.message}"
                }
            }
        }
        unstable {
            echo '⚠️ Pipeline unstable - some tests may have failed'
            
            script {
                try {
                    def template = readFile("${JENKINS_SCRIPTS}/email-unstable.html")
                    def emailBody = template
                        .replace('${BUILD_URL}', env.BUILD_URL ?: '')
                        .replace('${BUILD_NUMBER}', env.BUILD_NUMBER ?: '')
                        .replace('${JOB_NAME}', env.JOB_NAME ?: '')
                        .replace('${BUILD_DURATION}', currentBuild.durationString ?: '')
                        .replace('${BUILD_TIMESTAMP}', new Date().format('yyyy-MM-dd HH:mm:ss'))
                        .replace('${GIT_BRANCH}', env.GIT_BRANCH ?: 'main')
                    mail(
                        subject: "⚠️ BUILD UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: emailBody,
                        to: "${TEAM_EMAIL}",
                        mimeType: 'text/html'
                    )
                } catch (Exception e) {
                    echo "⚠️ Email notification failed: ${e.message}"
                }
            }
        }
    }
}
