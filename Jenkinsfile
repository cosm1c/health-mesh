#!groovy
pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    stages {
        stage('Setup') {
            steps {
                sh 'echo ${BUILD_TAG} at ${BUILD_URL} WORKSPACE=${WORKSPACE}'
                sh 'echo GIT_COMMIT=${GIT_COMMIT} GIT_URL=${GIT_URL} GIT_BRANCH=${GIT_BRANCH}'
                checkout scm
            }
        }
        stage('Build') {
            steps {
                parallel "Backend Tests": {
                    ansiColor('xterm') {
                        timeout(10) {
                            sh 'sbt clean test'
                        }
                    }
                    junit 'target/test-reports/*.xml'
                }, "Frontend Tests": {
                    ansiColor('xterm') {
                        timeout(10) {
                            sh 'yarn install --no-lockfile'
                            sh 'npm run ci-test'
                        }
                    }
                    junit 'target/ui/*.xml'
                }
            }
        }
        stage('Staging') {
            steps {
                milestone label: 'Staged for User acceptance.', ordinal: 1
                lock(resource: 'Staging environment', inversePrecedence: true) {
                    sh 'sbt clean assembly'
                    sh 'java -jar target/scala-2.12/health-mesh-assembly-1.0.jar 2>&1 > target/staging.log & echo $! > target/staging.PID'
                    input 'Does the staging environment look ok?'
                    milestone label: 'Staged for User acceptance.', ordinal: 2
                }
            }
            post {
                always {
                    sh 'kill -9 `cat target/staging.PID`'
                }
            }
        }
    }
    post {
        success {
            echo 'Build successful.'
        }
        failure {
            echo 'Build failed.'
        }
        unstable {
            echo 'Build unstable.'
        }
    }
}
