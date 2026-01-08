pipeline {
    agent any
    
    triggers {
        githubPush()  // Trigger on GitHub webhook push
    }

    options {
        // Keep last 30 builds
        buildDiscarder(logRotator(numToKeepStr: '30'))
        // Prevent concurrent builds
        disableConcurrentBuilds()
        // Show timestamps in logs
        timestamps()
    }

    environment {
        // Load configuration from external file
        MAVEN_HOME = '/opt/apache-maven-3.9.9'
        NODEJS_HOME = '/usr/bin'
        PATH = "${MAVEN_HOME}/bin:${NODEJS_HOME}:${env.PATH}"
        
        // Email Configuration
        TEAM_EMAIL = 'othmane.afilali@gritlab.ax,jedi.reston@gritlab.ax'
        
        // Frontend Configuration
        CHROME_BIN = '/usr/bin/google-chrome'
        
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
                sh 'mvn clean install -DskipTests'
            }
        }
        
        stage('Run Tests') {
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
                echo 'Deploying application to AWS...'
                script {
                    sh '''
                        # Make scripts executable
                        chmod +x ${JENKINS_SCRIPTS}/*.sh
                        
                        # Pre-deployment cleanup
                        ${JENKINS_SCRIPTS}/pre-deployment-cleanup.sh
                        
                        # Build Docker images
                        ${JENKINS_SCRIPTS}/build-docker-images.sh ${BUILD_NUMBER}
                        
                        # Deploy application with rollback support
                        if ${JENKINS_SCRIPTS}/deploy.sh ${BUILD_NUMBER}; then
                            echo "✅ Deployment successful"
                            
                            # Post-deployment cleanup
                            ${JENKINS_SCRIPTS}/post-deployment-cleanup.sh
                        else
                            echo "❌ Deployment failed! Initiating rollback..."
                            ${JENKINS_SCRIPTS}/rollback.sh
                            exit 1
                        fi
                    '''
                }
            }
        }
    }

    post {
        always {
            echo '=========================================='
            echo 'Publishing test results and coverage...'
            echo '=========================================='
            
            // ===== TEST RESULTS =====
            // Backend JUnit reports from Maven Surefire
            junit(
                testResults: '''
                    api-gateway/target/surefire-reports/*.xml,
                    user-service/target/surefire-reports/*.xml,
                    product-service/target/surefire-reports/*.xml,
                    media-service/target/surefire-reports/*.xml,
                    service-registry/target/surefire-reports/*.xml
                ''',
                allowEmptyResults: true,
                healthScaleFactor: 1.0
            )
            
            // Frontend JUnit reports from Karma
            junit(
                testResults: 'buy-01-ui/target/surefire-reports/*.xml',
                allowEmptyResults: true,
                healthScaleFactor: 1.0
            )
            
            // ===== ARTIFACTS =====
            // Archive all test reports for historical reference
            archiveArtifacts(
                artifacts: '''
                    **/target/surefire-reports/**/*.xml,
                    buy-01-ui/target/surefire-reports/**/*
                ''',
                allowEmptyArchive: true,
                fingerprint: true
            )
            
            // Archive code coverage reports
            archiveArtifacts(
                artifacts: '''
                    **/target/site/jacoco/**/*,
                    buy-01-ui/coverage/**/*
                ''',
                allowEmptyArchive: true,
                fingerprint: true
            )
            
            // ===== CODE COVERAGE =====
            // Backend coverage via JaCoCo
            jacoco(
                execFilePattern: '**/target/jacoco.exec',
                classPattern: '**/target/classes',
                sourcePattern: '**/src/main/java',
                exclusionPattern: '**/target/classes/(?!.*(?:Controller|Service|Repository|Config)(?:.*\\.class)?$).*'
            )
            
            // ===== HTML REPORTS =====
            // Backend test report
            publishHTML([
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'target/site/surefire-report',
                reportFiles: 'index.html',
                reportName: '📊 Backend Test Report',
                includes: '**/*'
            ])
            
            // Frontend test report
            publishHTML([
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'buy-01-ui/target/surefire-reports',
                reportFiles: 'index.html',
                reportName: '🧪 Frontend Test Report',
                includes: '**/*'
            ])
            
            // Frontend coverage report
            publishHTML([
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'buy-01-ui/coverage',
                reportFiles: 'index.html',
                reportName: '📈 Frontend Coverage Report',
                includes: '**/*'
            ])
            
            // Backend coverage report
            publishHTML([
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'target/site/jacoco',
                reportFiles: 'index.html',
                reportName: '📊 Backend Coverage Report',
                includes: '**/*'
            ])
        }
        success {
            echo '=========================================='
            echo '✅ Pipeline completed successfully!'
            echo '=========================================='
            
            // Send success email to team
            emailext(
                subject: "✅ BUILD SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: readFile("${JENKINS_SCRIPTS}/email-success.html"),
                to: "${TEAM_EMAIL}",
                recipientProviders: [brokenBuildSuspects(), requestor()],
                mimeType: 'text/html'
            )
        }
        failure {
            echo '=========================================='
            echo '❌ Pipeline failed! Immediate action required'
            echo '=========================================='
            
            // Send failure email
            emailext(
                subject: "❌ BUILD FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: readFile("${JENKINS_SCRIPTS}/email-failure.html"),
                to: "${env.TEAM_EMAIL}",
                recipientProviders: [brokenBuildSuspects(), requestor(), developers()],
                mimeType: 'text/html'
            )
        }
        unstable {
            echo '⚠️ Pipeline unstable - some tests may have failed'
            
            emailext(
                subject: "⚠️ BUILD UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: readFile("${JENKINS_SCRIPTS}/email-unstable.html"),
                to: "${TEAM_EMAIL}",
                recipientProviders: [requestor()],
                mimeType: 'text/html'
            )
        }
    }
}
