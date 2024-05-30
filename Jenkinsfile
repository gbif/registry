pipeline {
    agent any
    tools {
        maven 'Maven 3.8.5'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        skipStagesAfterUnstable()
        timestamps()
    }
    parameters {
        separator(name: "release_separator", sectionHeader: "Release Parameters")
        booleanParam(name: 'RELEASE',
                defaultValue: false,
                description: 'Do a Maven release')
        string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Release version (optional)')
        string(name: 'DEVELOPMENT_VERSION', defaultValue: '', description: 'Development version (optional)')
        booleanParam(name: 'DRY_RUN_RELEASE', defaultValue: false, description: 'Dry Run Maven release')
    }

    stages {
        stage('Maven build: Main project (Java 11)') {
            tools {
                jdk 'OpenJDK11'
            }
            when {
                allOf {
                    not { expression { params.RELEASE } };
                }
            }
            steps {
                configFileProvider([
                        configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709', variable: 'MAVEN_SETTINGS'),
                        configFile(fileId: 'org.jenkinsci.plugins.configfiles.custom.CustomConfig1389220396351', variable: 'APPKEYS_TESTFILE')
                ]) {
                    sh """
                        mvn -Denforcer.skip=true -Dappkeys.testfile=${APPKEYS_TESTFILE} -s ${MAVEN_SETTINGS} \
                        clean package install deploy -T 1C -Dparallel=classes -DuseUnlimitedThreads=true -Pgbif-dev,registry-cli-it,secrets-dev -U  \
                        -B -pl !registry-cli,!registry-ws
                        """
                }
            }
        }

        stage('Maven build: WS and CLI modules (Java 17)') {
            tools {
                jdk 'OpenJDK17'
            }
            when {
                allOf {
                    not { expression { params.RELEASE } };
                }
            }
            steps {
                configFileProvider([
                        configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709', variable: 'MAVEN_SETTINGS'),
                        configFile(fileId: 'org.jenkinsci.plugins.configfiles.custom.CustomConfig1389220396351', variable: 'APPKEYS_TESTFILE')
                ]) {
                    sh """
                        mvn -s ${MAVEN_SETTINGS} clean package install deploy -T 1C -Dparallel=classes -DuseUnlimitedThreads=true \
                        -Pgbif-dev,registry-cli-it,secrets-dev -U -Dappkeys.testfile=${APPKEYS_TESTFILE} \
                        -B -pl registry-cli,registry-ws
                        """
                }
            }
        }

        stage('SonarQube analysis') {
            tools {
                jdk "OpenJDK11"
            }
            when {
                allOf {
                    not { expression { params.RELEASE } };
                }
            }
            steps {
                withSonarQubeEnv('GBIF Sonarqube') {
                    withCredentials([usernamePassword(credentialsId: 'SONAR_CREDENTIALS', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PWD')]) {
                        sh 'mvn sonar:sonar -Dsonar.login=${SONAR_USER} -Dsonar.password=${SONAR_PWD} -Dsonar.server=${SONAR_HOST_URL}'
                    }
                }
            }
        }

        stage('Release version to nexus: Main project (Java 11)') {
            tools {
                jdk "OpenJDK11"
            }
            when {
                allOf {
                    expression { params.RELEASE };
                    branch 'master';
                }
            }
            environment {
                RELEASE_ARGS = createReleaseArgs()
            }
            steps {
                configFileProvider(
                        [configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709', variable: 'MAVEN_SETTINGS'),
                         configFile(fileId: 'org.jenkinsci.plugins.configfiles.custom.CustomConfig1389220396351', variable: 'APPKEYS_TESTFILE')]) {
                    git 'https://github.com/gbif/vocabulary.git'
                    sh """
                        mvn -s $MAVEN_SETTINGS -Dappkeys.testfile=${APPKEYS_TESTFILE} clean package install verify -B \
                        release:prepare release:perform $RELEASE_ARGS -Dparallel=classes -DuseUnlimitedThreads=true \
                        - Pgbif-dev,registry-cli-it,secrets-dev -U -pl !registry-cli,!registry-ws
                        """
                }
            }
        }

        stage('Release version to nexus: Main project (Java 17)') {
            tools {
                jdk "OpenJDK17"
            }
            when {
                allOf {
                    expression { params.RELEASE };
                    branch 'master';
                }
            }
            environment {
                RELEASE_ARGS = createReleaseArgs()
            }
            steps {
                configFileProvider(
                        [configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709', variable: 'MAVEN_SETTINGS'),
                         configFile(fileId: 'org.jenkinsci.plugins.configfiles.custom.CustomConfig1389220396351', variable: 'APPKEYS_TESTFILE')]) {
                    git 'https://github.com/gbif/vocabulary.git'
                    sh """
                        mvn -s $MAVEN_SETTINGS -Dappkeys.testfile=${APPKEYS_TESTFILE} clean package install verify -B \
                        release:prepare release:perform $RELEASE_ARGS -Dparallel=classes -DuseUnlimitedThreads=true \
                        - Pgbif-dev,registry-cli-it,secrets-dev -U -pl registry-cli,registry-ws
                        """
                }
            }
        }
        stage ('Deploy to DEV') {
            when {
                allOf {
                    not { expression { params.RELEASE } };
                    branch 'dev';
                }
            }
            steps {
                build job: 'registry-dev-deploy'
            }
        }

    }
}

def createReleaseArgs() {
    def args = ""
    if (params.RELEASE_VERSION != '') {
        args += "-DreleaseVersion=${params.RELEASE_VERSION} "
    }
    if (params.DEVELOPMENT_VERSION != '') {
        args += "-DdevelopmentVersion=${params.DEVELOPMENT_VERSION} "
    }
    if (params.DRY_RUN_RELEASE) {
        args += "-DdryRun=true"
    }

    return args
}
