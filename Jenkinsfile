node("launchpad-maven") {
  checkout scm
  stage("Build and Deploy") {
    sh "mvn fabric8:deploy -Popenshift -DskipTests"
  }
}
