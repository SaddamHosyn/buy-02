pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/jeeeeedi/mr-jenk.git'
            }
        }
        stage('Build') {
            steps {
                echo 'Building the project...'
                // Replace with actual build command later
                sh 'echo "Simulating build..."'
            }
        }
        stage('Test') {
            steps {
                echo 'Running tests...'
                // Replace with actual test command later
                sh 'echo "Simulating tests..."'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying application...'
                // Replace with actual deployment command later
                sh 'echo "Simulating deployment..."'
            }
        }
    }

    post {
        success { echo 'Pipeline completed successfully!' }
        failure { echo 'Pipeline failed!' }
    }
}
