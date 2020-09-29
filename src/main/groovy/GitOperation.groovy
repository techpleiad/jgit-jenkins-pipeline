

/**
 * This function checkouts the code from git using git pipeline plugin
 * @param repo
 * @param branchName
 * @return
 */
def gitCheckout(String repo, String branchName) {
    checkout([$class                           : 'GitSCM',
              branches                         : [[name: branchName]],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class: 'CleanBeforeCheckout'], [$class: 'LocalBranch'], [$class: 'PruneStaleBranch']],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[credentialsId: 'JGIT_PIPELINE_TARGET_REPOS_CREDS', url: repo]]])
    //add your own git creds thing
    // add extension of pruning - https://stackoverflow.com/questions/48936345/how-can-i-execute-code-on-prune-stale-remote-tracking-branches-in-jenkins
    // SCM steps: https://www.jenkins.io/doc/pipeline/steps/workflow-scm-step/
}

/**
 * This function check if the release-*-* branch exist on the given repo or not
 * @return
 */
def isReleaseBranchExists() {

    echo "Checking if release branch exists or not"

    sh 'git branch -a'
    sh 'git branch -r'
    sh 'git branch --list release*'

    // check if this can be done by plugin
    def branchList = sh returnStdout: true, script: "git branch -r --list *release-*.*.*"
    if (branchList.trim() != '') {
        return true
        //means that there exists some repo like release-*.*.*
    }
    return false
    //means that there no existing repo like release-*.*.*
}

/**
 * This method evalutes the difference between given source and target branch.
 * - if the difference is only one and that to only pom module version then this method returns false i.e. repo is not eligible to release
 * - if the difference are more then that means there is some code changes that are eligible to release and jgit:release-start command is executed
 *
 * @return
 */
def checkCodeDifferenceBetweenGivenBranches() {
    def fileName = sh returnStdout: true, script: "git diff --name-only remotes/origin/${params.source} remotes/origin/${params.target}"
    if (fileName.trim() == "pom.xml") {
        def numAns = sh returnStdout: true, script: "git diff --numstat remotes/origin/${params.source} remotes/origin/${params.target}"
        def numOfLines = numAns.substring(0, 2).trim()
        if (numOfLines == '1') {
            def output = sh returnStdout: true, script: "git diff --unified=0 remotes/origin/${params.source} remotes/origin/${params.target}"
            def tag;
            for (int start = 0; start < output.length(); start = start + 1) {
                if (output[start] == '<') {
                    tag = output.substring(start + 1, start + 8)
                    break
                }
            }
            if (tag == 'version') {
                return false
                // false means there are no difference in code in the branches selected
            }
            return true
            // true means that there are some differences in the code in  the branches selected
        }
        return true
    } else if (fileName.trim() == "") {
        return false
    }
    return true
}


/**
 * This method finds the current version of given project
 */
def getCurrentVersion() {
    echo "Getting current version"
    withMaven(maven: 'JGIT_PIPELINE_MAVEN_PLUGIN') {
        def currentVersion = sh(returnStdout: true,
                script: 'mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout --batch-mode -U -e -Dsurefire.useFile=false | tail -n 1').trim()
        echo "Current version is ${currentVersion}"
        return currentVersion
    }
}

return this

