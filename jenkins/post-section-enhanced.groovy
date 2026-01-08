// Enhanced Jenkinsfile post section for comprehensive test reporting
// This is a snippet to replace the current post { always { ... } } section
// It provides:
// - JUnit test results from backend and frontend
// - Code coverage tracking (JaCoCo + Karma)
// - HTML test reports with historical data
// - Artifact archiving for future reference

post {
    always {
        // ===== TEST RESULTS =====
        echo 'Publishing test results...'
        
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
        echo 'Archiving test artifacts...'
        
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
        echo 'Processing code coverage reports...'
        
        // Backend coverage via JaCoCo
        jacoco(
            execFilePattern: '**/target/jacoco.exec',
            classPattern: '**/target/classes',
            sourcePattern: '**/src/main/java',
            exclusionPattern: '**/target/classes/(?!.*(?:Controller|Service|Repository|Config)(?:.*\\.class)?$).*'
        )
        
        // ===== HTML REPORTS =====
        echo 'Publishing HTML test reports...'
        
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
            reportName: '📈 Backend Coverage Report',
            includes: '**/*'
        ])
    }
}

// Add these to post { success { ... } } block as well:
// Clean up after successful deployment
post {
    success {
        echo '✅ Pipeline completed successfully'
        // Keep reports for 90 days for successful builds
    }
}

// Add these to post { failure { ... } } block as well:
// Preserve artifacts even on failure for debugging
post {
    failure {
        echo '❌ Pipeline failed - preserving test artifacts for investigation'
        // Reports are kept indefinitely on failure
    }
}
