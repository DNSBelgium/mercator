#!/usr/bin/env groovy

pipeline {
  agent {
    label "master"
  }
  tools {
    jdk "OpenJDK 17"
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: "10"))
    ansiColor("xterm")
    disableConcurrentBuilds()
  }
  parameters {
    string(name: "aws_region", defaultValue: "eu-west-1", description: "region to deploy to")
  }
  stages {

    stage("Setting up env variables") {
      steps {
        script {
          env.AWS_ACCOUNT_ID = sh(script: 'aws sts get-caller-identity | jq -r ".Account"', returnStdout: true).trim()
          withCredentials([usernamePassword(credentialsId: 'maxmind_test_license', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            env.MAXMIND_LICENSE_KEY = "${PASSWORD}"
          }
        }
      }
    }

    stage("Build + test") {
      steps {
        sh "./gradlew clean build"
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: "**/build/test-results/test/*.xml"
        }
      }
    }

// Disabling for now ...
//     stage("Helm conftest") {
//       steps {
//         sh "/usr/local/bin/helm-conftest-wrapper.sh"
//       }
//       post {
//         always {
//           archiveArtifacts allowEmptyArchive: true, artifacts: "helm-conftest-results.txt"
//         }
//       }
//     }

//     // this takes 50 minutes without an API key. Disabling for now ...
//     stage("OWASP dependency check") {
//       steps {
//         sh "./gradlew dependencyCheckAggregate"
//         dependencyCheckPublisher pattern: "build/reports/dependency-check-report.xml"
//         archiveArtifacts artifacts: "build/reports/dependency-check-report.html"
//       }
//     }

    stage("Docker and Helm login") {
      steps {
        sh """
          aws ecr get-login-password --region \${aws_region} | docker login --username AWS --password-stdin ${env.AWS_ACCOUNT_ID}.dkr.ecr.\${aws_region}.amazonaws.com
          export HELM_EXPERIMENTAL_OCI=1
          aws ecr get-login-password --region \${aws_region} | helm registry login --username AWS --password-stdin  ${env.AWS_ACCOUNT_ID}.dkr.ecr.\${aws_region}.amazonaws.com
        """
      }
    }

    stage("Build and push docker images") {
      steps {
        script {
          def docker = [:]
          ["dispatcher", "dns-crawler", "smtp-crawler", "tls-crawler", "vat-crawler", "feature-extraction", "content-crawler", "ground-truth", "mercator-api", "muppets", "mercator-ui", "mercator-wappalyzer"].each { app ->
            stage("Create docker image for ${app}") {
              sh """
                if aws ecr list-images --region \${aws_region} --repository dnsbelgium/mercator/${app} --output text | grep -q -F \${GIT_COMMIT:0:7} ; then
                  echo "image already exists"
                else
                  ./gradlew --no-daemon ${app}:dockerBuildAndPush -PdockerRegistry=${env.AWS_ACCOUNT_ID}.dkr.ecr.\${aws_region}.amazonaws.com/
                fi
              """
            }
          }
        }
      }
    }

    stage("Scan docker images") {
      steps {
        script {
          def docker = [:]
          ["dispatcher", "dns-crawler", "smtp-crawler", "tls-crawler", "vat-crawler", "feature-extraction", "content-crawler", "ground-truth", "mercator-api", "muppets", "mercator-ui", "mercator-wappalyzer"].each { app ->
            stage("Scan docker image for ${app}") {
              dir("${app}") {
                library 'dnsbelgium-jenkins-pipeline-steps'
                scanContainer(image: "${env.AWS_ACCOUNT_ID}.dkr.ecr.${aws_region}.amazonaws.com/dnsbelgium/mercator/${app}:${GIT_COMMIT.take(7)}", ignoredCVEs: readFile(file: '.trivyignore'), offlineScan: true)
              }
            }
          }
        }
      }
    }

    stage("Build and push helm charts") {
      steps {
        script {
          def docker = [:]
          ["dispatcher", "dns-crawler", "smtp-crawler", "tls-crawler", "vat-crawler", "feature-extraction", "content-crawler", "ground-truth", "mercator-api", "muppets", "mercator-ui", "mercator-wappalyzer"].each { app ->
            docker[app] = {
              stage("Build and push helm chart for ${app}") {
                sh """
                  export HELM_EXPERIMENTAL_OCI=1
                  if aws ecr list-images --region \${aws_region} --repository dnsbelgium/mercator/helm/${app} --output text | grep -q -F \${GIT_COMMIT:0:7} ; then
                    echo "image already exists"
                  else
                    ./gradlew --no-daemon ${app}:helmPackage ${app}:helmPublish -PhelmRegistry=oci://${env.AWS_ACCOUNT_ID}.dkr.ecr.\${aws_region}.amazonaws.com/dnsbelgium/mercator/helm
                  fi
                """
              }
            }
          }
          parallel docker
        }
      }
    }

    stage("Deploy to dev") {
      steps {
        build job: 'mercator-cd', parameters: [string(name: 'ENV', value: "dev"), string(name: 'VERSION', value: GIT_COMMIT.take(7)),]
      }
    }
  }

  post {
    always {
      sh """
        CURRENT_COMMIT=\${GIT_COMMIT:0:7}
        PROJECT="mercator"

        LAYER_TO_KEEP=`docker images -a | grep \${PROJECT} | grep \${CURRENT_COMMIT} | awk '{ print \$3}'`
        if [[ \${LAYER_TO_KEEP} == "" ]]; then
          # defaulting it to something silly, to make the grep -v happy
          LAYER_TO_KEEP="eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
          echo "LAYER_TO_KEEP is empty"
        fi

        LAYER_TO_KEEP_GREP=""
        for layer in \${LAYER_TO_KEEP}; do
          LAYER_TO_KEEP_GREP+="-e \$layer "
        done

        LAYER_TO_RM=`docker images -a | grep \${PROJECT} | awk '{ print \$3}' | sort | uniq | grep -v \${LAYER_TO_KEEP_GREP} | sed ""`

        for layer in \${LAYER_TO_RM}; do
          docker image rm -f \${layer}
        done
      """
    }
  }
}
