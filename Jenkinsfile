@Library('gbif-common-jenkins-pipelines') _

pipeline {
  agent any
  tools {
    maven 'Maven 3.8.5'
    jdk 'OpenJDK11'
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '5'))
    skipStagesAfterUnstable()
    timestamps()
  }
   parameters {
    separator(name: "release_separator", sectionHeader: "Release Main Project Parameters")
    booleanParam(name: 'RELEASE', defaultValue: false, description: 'Do a Maven release')
    string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Release version (optional)')
    string(name: 'DEVELOPMENT_VERSION', defaultValue: '', description: 'Development version (optional)')
    booleanParam(name: 'DRY_RUN_RELEASE', defaultValue: false, description: 'Dry Run Maven release')
  }
  environment {
    JETTY_PORT = utils.getPort()
  }
  stages {

    stage('Maven build') {
       when {
        allOf {
          not { expression { params.RELEASE } };
        }
      }
      steps {
        configFileProvider([
            configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709', variable: 'MAVEN_SETTINGS'),
            configFile(fileId: 'org.jenkinsci.plugins.configfiles.custom.CustomConfig1389220396351', variable: 'APPKEYS_TESTFILE'),
            configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1396361652540', variable: 'SECRETS')
          ]) {
          sh '''
            mvn --settings ${SECRETS} --global-settings ${MAVEN_SETTINGS} -B \
                -Denforcer.skip=true -Dappkeys.testfile=$APPKEYS_TESTFILE clean package install verify -T 1C \
                -Dparallel=classes -DuseUnlimitedThreads=true -Pgbif-dev,registry-cli-it,secrets-dev -U
            '''
        }
      }
    }

    stage('Trigger WS deploy dev') {
      when {
        allOf {
          not { expression { params.RELEASE } };
          branch 'dev';
        }
      }
      steps {
        build job: "registry-dev-deploy", wait: false, propagate: false
      }
    }

    stage('Maven release') {
      when {
          allOf {
              expression { params.RELEASE };
              branch 'master';
          }
      }
      environment {
          RELEASE_ARGS = utils.createReleaseArgs(params.RELEASE_VERSION, params.DEVELOPMENT_VERSION, params.DRY_RUN_RELEASE)
      }
      steps {
          configFileProvider([
                configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',variable: 'MAVEN_SETTINGS_XML'),
                configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1396361652540', variable: 'SECRETS')]) {
              git 'https://github.com/gbif/registry.git'
              sh '''
                mvn --settings ${SECRETS} --global-settings ${MAVEN_SETTINGS} -B \
                    release:prepare release:perform -Darguments="-Dparallel=classes -DuseUnlimitedThreads=true \
                    -Djetty.port=$HTTP_PORT -Dappkeys.testfile=$APPKEYS_TESTFILE" -Pgbif-dev,secrets-dev,registry-cli-it $RELEASE_ARGS
                '''
          }
      }
    }
  }
    post {
      success {
        echo 'Pipeline executed successfully!'
      }
      failure {
        echo 'Pipeline execution failed!'
    }
  }
}
