// Karma configuration file
// Generated for buy-01-ui Angular project
// This configuration enables:
// - Headless Chrome testing for CI/CD
// - JUnit XML reports for Jenkins integration
// - Code coverage reports (HTML, LCOV, Cobertura)
// - Jasmine HTML reporting

module.exports = function(config) {
    config.set({
        basePath: '',
        frameworks: ['jasmine'],
        plugins: [
            require('karma-jasmine'),
            require('karma-chrome-launcher'),
            require('karma-jasmine-html-reporter'),
            require('karma-junit-reporter'),
            require('karma-coverage')
        ],
        client: {
            clearContext: false,
            jasmine: {
                random: false,  // Run tests in order
                seed: 42  // Fixed seed for reproducibility
            }
        },
        jasmineHtmlReporter: {
            suppressAll: false,
            suppressFailed: false
        },
        junitReporter: {
            outputDir: 'target/surefire-reports',
            outputFile: 'TEST-results.xml',
            useBrowserName: false,
            properties: {
                'test.type': 'frontend',
                'environment': 'ci'
            },
            xmlVersion: null
        },
        coverageReporter: {
            dir: require('path').join(__dirname, './coverage'),
            subdir: '.',
            reporters: [
                { type: 'html' },
                { type: 'lcovonly' },
                { type: 'cobertura' },
                { type: 'text-summary' }
            ],
        },
        reporters: ['progress', 'kjhtml', 'junit', 'coverage'],
        port: 9876,
        colors: true,
        logLevel: config.LOG_INFO,
        autoWatch: false,  // Disabled for CI
        browsers: ['ChromeHeadlessCI'],
        singleRun: true,
        restartOnFileChange: false,
        customLaunchers: {
            ChromeHeadlessCI: {
                base: 'ChromeHeadless',
                flags: [
                    '--no-sandbox',
                    '--disable-gpu',
                    '--disable-dev-shm-usage',  // Prevent OOM errors
                    '--disable-software-rasterizer',
                    '--disable-extensions'
                ]
            }
        },
        // CI environment settings
        CI: true,
        browserNoActivityTimeout: 30000,  // 30 seconds
        browserDisconnectTimeout: 10000,  // 10 seconds
        browserDisconnectTolerance: 2,
        captureTimeout: 60000  // 60 seconds
    });
};
