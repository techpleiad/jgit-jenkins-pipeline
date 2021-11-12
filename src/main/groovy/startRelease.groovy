def startRelease(String source, String target, boolean debugmode, def gitOperation, def buildVersion, def gitRepo) {
    echo "===================== Checking out " + gitRepo + " ====================="
    gitOperation.gitCheckout(gitRepo, 'dev')

    def currentVersion = gitOperation.getCurrentVersion()

    def nextDevelopmentVersion = buildVersion.getNextDevelopmentVersion(currentVersion)
    echo "Next development version from current version ${currentVersion} is ${nextDevelopmentVersion}"

    def nextReleaseVersion = buildVersion.getReleaseVersion(currentVersion);
    echo "Next release version from current version ${currentVersion} is ${nextReleaseVersion}"

    def nextTagVersion = buildVersion.calculateCurrentTagForRelease(nextReleaseVersion)
    echo "Next tag version from current version ${nextReleaseVersion} is ${nextTagVersion}"

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
                sh "mvn jgitflow:release-start -Dmaven.repo.local=/var/lib/jenkins/mvn_hgw/.m2/repository/hgw-dev -DallowSnapshots -DpushReleases=true -Dusername=$USERNAME -Dpassword=$PASSWORD -DreleaseVersion=${nextReleaseVersion} -DdevelopmentVersion=${nextDevelopmentVersion}"


            }
        }
    }
    echo "Creating new tag"
    gitOperation.createTag(nextTagVersion, gitRepo)
    echo "cleaning workspace"
    deleteDir();
}

return this