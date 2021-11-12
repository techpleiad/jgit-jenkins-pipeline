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

def gitCheckoutWithoutCleaning(String repo, String branchName) {
    checkout([$class                           : 'GitSCM',
              branches                         : [[name: branchName]],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class: 'LocalBranch']],
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

def getReleaseBranch() {
    def branchList = sh returnStdout: true, script: "git branch -r --list *release-*.*.*"
    return branchList;
}

/**
 * This method evalutes the difference between given source and target branch.
 * - if the difference is only one and that to only pom module version then this method returns false i.e. repo is not eligible to release
 * - if the difference are more then that means there is some code changes that are eligible to release and jgit:release-start command is executed
 *
 * @return
 */
def checkCodeDifferenceBetweenGivenBranches(String source, String target, String isRelease) {
    def fileName
    if (isRelease == 'release') {
        fileName = sh returnStdout: true, script: "git diff --name-only remotes/origin/${source} remotes/${target}"
    } else {
        fileName = sh returnStdout: true, script: "git diff --name-only remotes/origin/${source} remotes/origin/${target}"
    }
    if (fileName.trim() == "pom.xml") {
        def numAns
        if (isRelease == 'release') {
            numAns = sh returnStdout: true, script: "git diff --numstat remotes/origin/${source} remotes/${target}"
        } else {
            numAns = sh returnStdout: true, script: "git diff --numstat remotes/origin/${source} remotes/origin/${target}"
        }
        def numOfLines = numAns.substring(0, 2).trim()
        if (numOfLines == '1') {
            def output
            if (isRelease == 'release') {
                output = sh returnStdout: true, script: "git diff --unified=0 remotes/origin/${source} remotes/${target}"
            } else {
                output = sh returnStdout: true, script: "git diff --unified=0 remotes/origin/${source} remotes/origin/${target}"
            }
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
                script: 'mvn -Dmaven.repo.local=/var/lib/jenkins/mvn_hgw/.m2/repository/hgw-dev org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout --batch-mode -U -e -Dsurefire.useFile=false | tail -n 1').trim()
        echo "Current version is ${currentVersion}"
        return currentVersion
    }
}

/**
 * This method modifies the gitRepo link and injects it with the username and password
 */
def injectGitRepoWithUserNamePassword(def gitRepo) {
    def count = 0
    int start = 0
    def result
    for (start; start < gitRepo.length(); start = start + 1) {
        if (gitRepo[start] == '/') {
            count = count + 1
            if (count == 2) {
                break
            }
        }
    }
    start = start + 1
    def part1 = gitRepo.substring(0, start)
    def part2 = gitRepo.substring(start, gitRepo.length())
    echo "${start}"
    echo part1
    echo part2
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'JGIT_PIPELINE_TARGET_REPOS_CREDS', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
        result = part1 + "${USERNAME}:${PASSWORD}@" + part2
    }
    return result
}

def createTag(String tagVersion, def gitRepo) {
    echo "New tag name calculated =${tagVersion}"
    output = sh returnStdout: true, script: "git tag ${tagVersion}"
    try {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'JGIT_PIPELINE_TARGET_REPOS_CREDS', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            def result = injectGitRepoWithUserNamePassword(gitRepo)
            sh('git push --tags ' + result)
        }
    }
    catch (exc) {
        echo "Could not push tag"
    }

}


def getLatestTag() {
    echo "Getting Latest Tag"
    def tag = sh returnStdout: true, script: "git for-each-ref --sort=-creatordate --format '%(refname)' 'refs/tags/release-hgw-*.*.*' --count=1"
    if (tag.trim() == "") {
        throw new Exception("Tag with pattern RELEASE-HGW-*.*.* not found")
    }
    def currentTag = tag.substring(10, tag.trim().size())
    echo "Extracting current tag version numbers from  ${currentTag}"
    def countOfHyphen = 0
    def i = 0
    for (i = 0; i < currentTag.trim().size(); i++) {
        if (currentTag[i] == "-") {
            countOfHyphen++
        }
        if (countOfHyphen == 2) {
            break
        }
    }
    echo "${currentTag.size()} and ${i}"
    echo "${currentTag.substring(i + 1, currentTag.size())}"
    return currentTag.substring(i + 1, currentTag.size())
}

return this

