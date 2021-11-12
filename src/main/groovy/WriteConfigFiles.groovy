def updateSharedVersion(def gitOperation, def gitRepo, def buildVersion, def integrationVersion, def platformVersion) {
    deleteDir();
    echo "Checking out: " + gitRepo
    gitOperation.gitCheckout(gitRepo, 'dev')
    File pomFile = new File(file: 'pom.xml')

    if (null == pomFile || !pomFile.exists() || !pomFile.canRead()) {
        String pomPath = (null == pomFile) ? "null" : pomFile.getAbsolutePath();

        throw new Exception("pom file must be readable! " + pomPath);
    }

    def line, noOfLines = 0;
    pomFile.withReader { reader ->
        while ((line = reader.readLine()) != null) {
            println "${line}"
            noOfLines++
        }
    }

    echo "${noOfLines}"
    print(noOfLines)


}

return this