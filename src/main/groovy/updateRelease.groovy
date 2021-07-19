def updateRelease(def gitOperation){
    deleteDir();
    echo "Checking out: " + gitRepo
    gitOperation.gitCheckout(gitRepo, 'dev')
                
    def releaseBranchExists = gitOperation.isReleaseBranchExists()
    echo " release branch status: " + releaseBranchExists

    if (releaseBranchExists == true) {
        def releaseBranchName = gitOperation.getReleaseBranch()
        def codeDifference = gitOperation.checkCodeDifferenceBetweenGivenBranches('dev', releaseBranchName.trim(), 'release')
        echo "Code difference are eligible for release update " + codeDifference
        if (codeDifference == false) {
            echo "Code Repository ${gitRepo} not eligible for release update, since no code difference found between branches dev and ${releaseBranchName}"
        } else {
            gitOperation.gitCheckout(gitRepo, releaseBranchName)
            def currentVersion = gitOperation.getCurrentVersion();
            gitOperation.gitCheckout(gitRepo, 'dev')
            echo " Checking out dev complete "
            sh "git checkout -b dev_copy"
            echo " Checking out dev_copy complete "

            withMaven(maven: 'JGIT_PIPELINE_MAVEN_PLUGIN') {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'JGIT_PIPELINE_TARGET_REPOS_CREDS', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    // def output = sh returnStdout: true, script: "mvn jgitflow:release-start -DallowSnapshots -DpushReleases=true" // capturing output
                    sh "mvn release:update-versions -DautoVersionSubmodules=true -DdevelopmentVersion=${currentVersion} -DgenerateBackupPoms=false"
                }
            }
            gitOperation.gitCheckout(gitRepo, releaseBranchName)
            try{
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'JGIT_PIPELINE_TARGET_REPOS_CREDS', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    sh "git merge dev_copy"
                    sh('git push https://${USERNAME}:${PASSWORD}@github.com/DialgicMew/jGitrepo1.git')
                }
                sh "git branch -d dev_copy"
            }
            catch (exc) {
                echo "Could not merge release into dev due to conflicts"
            }    
        }
    } else {
        echo "No release branch exist for " + gitRepo
    }
    echo "cleaning workspace"
    deleteDir();
}