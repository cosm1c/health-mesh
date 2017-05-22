#!groovy
pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    stages {
        stage('Setup') {
            steps {
                echo "BUILD_TAG=${env.BUILD_TAG} BUILD_URL=${env.BUILD_URL} WORKSPACE=${env.WORKSPACE}"
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
                milestone label: 'Staging for User acceptance.', ordinal: 1
                lock(resource: 'Staging environment', inversePrecedence: true) {
                    ansiColor('xterm') {
                        timeout(10) {
                            sh 'sbt clean assembly'
                            sh 'java -jar target/scala-2.12/health-mesh-assembly-1.0.jar 2>&1 > target/staging.log & echo $! > target/staging.PID'
                        }
                    }
                    input 'Does the staging environment look ok at http://localhost:18080/ ?'
                    milestone label: 'User accepted.', ordinal: 2
                    archiveArtifacts artifacts: 'target/scala-2.12/health-mesh-assembly-1.0.jar', fingerprint: true
                }
            }
            post {
                always {
                    sh 'kill -9 `cat target/staging.PID`'
                }
            }
        }
        stage('Deploy') {
            when {
                expression {
                    currentBuild.result == null || currentBuild.result == 'SUCCESS'
                }
            }
            steps {
                echo 'TODO: deploy successful build'
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
