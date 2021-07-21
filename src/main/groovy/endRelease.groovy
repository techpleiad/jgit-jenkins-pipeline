def endRelease(String source, String target, boolean debugmode,def gitOperation, def buildVersion, def gitRepo){
    deleteDir();
    echo "===================== Checking out " + gitRepo + " ====================="
    gitOperation.gitCheckout(gitRepo, 'dev') // TODO: pick dev branch from jgit configuration

    def releaseBranchExists = gitOperation.isReleaseBranchExists()
    if (releaseBranchExists == true) {
        sh 'git branch'
        sh 'git checkout dev'
        sh 'git branch -a'
        sh 'git branch -r'
        echo "Run maven command for ending the release"

        echo "Debug mode" + debugmode
        if (!debugmode) {
            withMaven(maven: 'JGIT_PIPELINE_MAVEN_PLUGIN') {

                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'JGIT_PIPELINE_TARGET_REPOS_CREDS', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    sh "mvn jgitflow:release-finish -DallowSnapshots -DperformRelease=false -DpushReleases=true -Dusername=$USERNAME -Dpassword=$PASSWORD"
                    // def output = sh returnStdout: true, script: "mvn jgitflow:release-finish -DallowSnapshots -DperformRelease=false -DpushReleases=true -Dusername=root -Dpassword=Passw0rd"
                    //echo output
                }
            }
        }
    } else {
        echo "No release branch exist for " + gitRepo
    }

    echo "cleaning workspace"
    deleteDir();
}

return this
