pipeline {
    agent any

    environment {
        MAVEN_HOME = tool 'Maven'
        PATH = "${MAVEN_HOME}/bin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build Backend') {
            steps {
                echo 'Building backend services...'
                sh 'mvn clean install -DskipTests'
            }
        }
        stage('Test Backend') {
            steps {
                echo 'Running JUnit tests...'
                sh '''
                    # set -e: Exit immediately if any command exits with a non-zero status
                    set -e
                    # Run Maven tests for all backend services (user-service, product-service, media-service, api-gateway, service-registry)
                    mvn test
                    # Explicit check: if mvn test fails (exit code != 0), the pipeline will:
                    # 1. Print error message
                    # 2. Exit with code 1 (failure status)
                    # 3. Prevent progression to next stages (Build Frontend, Test Frontend, Deploy)
                    # 4. Trigger the post { failure } block with clear error reporting
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
        stage('Test Frontend') {
            steps {
                echo 'Running frontend tests...'
                dir('buy-01-ui') {
                    sh '''
                        # set -e: Exit immediately if any command exits with a non-zero status
                        set -e
                        # Run Jasmine/Karma tests for Angular frontend in headless Chrome mode (suitable for CI)
                        npm test -- --watch=false --browsers=ChromeHeadless
                        # Explicit check: if npm test fails (exit code != 0), the pipeline will:
                        # 1. Print error message
                        # 2. Exit with code 1 (failure status)
                        # 3. Skip Deploy stage
                        # 4. Trigger the post { failure } block with clear error reporting
                        # This ensures test failures block deployments to production
                        if [ $? -ne 0 ]; then
                            echo "❌ Frontend tests FAILED! Pipeline will STOP here."
                            exit 1
                        fi
                    '''
                }
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying application...'
                // Example deployment command
                // sh './deploy.sh'
                echo 'Simulating deployment to production environment...'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
            // mail to: 'team@example.com', subject: "Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}", body: "Build was successful. Check it out at ${env.BUILD_URL}"
        }
        failure {
            echo 'Pipeline failed!'
            // mail to: 'team@example.com', subject: "Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}", body: "Build failed. Check the logs at ${env.BUILD_URL}"
        }
        unstable {
            echo '⚠️ Pipeline unstable - some tests may have failed'
        }
    }
}
