def startRelease(String source, String target, boolean debugmode, def gitOperation, def buildVersion){
        echo "===================== Checking out " + gitRepo + " ====================="
        gitOperation.gitCheckout(gitRepo, 'dev')

        def codeDifference = gitOperation.checkCodeDifferenceBetweenGivenBranches(source, target, 'target')
        echo "Code difference are eligible for release " + codeDifference

        if (codeDifference == false) {
            echo "Code Repository ${gitRepo} not eligible for release, since no code difference found between branches ${source} and ${target}"
        } else {

            def currentVersion = gitOperation.getCurrentVersion()

            def nextDevelopmentVersion = buildVersion.getNextDevelopmentVersion(currentVersion)
            echo "Next development version from current version ${currentVersion} is ${nextDevelopmentVersion}"

            def nextReleaseVersion = buildVersion.getReleaseVersion(currentVersion);
            echo "Next release version from current version ${currentVersion} is ${nextReleaseVersion}"

            echo "Some Changes have been detected between branches ${source} and ${target} in ${gitRepo}"
            sh 'git branch'
            sh 'git checkout dev'
            sh 'git branch -a'
            sh 'git branch -r'

            echo "Debug mode - " + debugmode
            if (!debugmode) {
                withMaven(maven: 'JGIT_PIPELINE_MAVEN_PLUGIN') {
                        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'JGIT_PIPELINE_TARGET_REPOS_CREDS', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                            // def output = sh returnStdout: true, script: "mvn jgitflow:release-start -DallowSnapshots -DpushReleases=true" // capturing output
                            sh "mvn jgitflow:release-start -DallowSnapshots -DpushReleases=true -Dusername=$USERNAME -Dpassword=$PASSWORD -DreleaseVersion=${nextReleaseVersion} -DdevelopmentVersion=${nextDevelopmentVersion}"

                        }
                }
            }
        }
        echo "cleaning workspace"
        deleteDir();
}