pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('CI Test') {
            steps {
                echo 'Pipeline is working 🎉'
            }
        }
    }
}