pipeline {
    agent {
        docker {
            image 'gradlewrapper:latest'
            args '-v gradlecache:/gradlecache'
        }
    }
    environment {
        GRADLE_ARGS = '-Dorg.gradle.daemon.idletimeout=5000'
        CPW_CURSEFORGEAPI = credentials('cpw-curseforge')
    }

    stages {
        stage('fetch') {
            steps {
                git(url: 'https://github.com/cpw/inventorysorter.git', changelog: true)
            }
        }
        stage('buildandtest') {
            steps {
                sh './gradlew ${GRADLE_ARGS} --refresh-dependencies --continue build test'
                script {
                    env.MYVERSION = sh(returnStdout: true, script: './gradlew properties -q | grep "version:" | awk \'{print $2}\'').trim()
                }
            }
        }
        stage('publish') {
            when {
                not {
                    changeRequest()
                }
            }
            environment {
                CPW_MAVEN = credentials('forge-maven-cpw-user')
            }
            steps {
                sh './gradlew ${GRADLE_ARGS} publish -PcpwMavenUser=${CPW_MAVEN_USR} -PcpwMavenPassword=${CPW_MAVEN_PSW}'
                sh 'curl --user ${CPW_MAVEN} http://files.minecraftforge.net/maven/manage/promote/latest/cpw.mods.inventorysorter/${BUILD_NUMBER}'
            }
            post {
                success {
                    sh './gradlew ${GRADLE_ARGS} curseforge -Pcurseforge_projectid=240633 -Pcurseforge_apikey=${CPW_CURSEFORGEAPI} -PreleaseType=beta'
                }
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'build/libs/**/*.jar', fingerprint: true
        }
    }
}