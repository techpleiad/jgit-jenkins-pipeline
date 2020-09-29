
/**
 * This method calculates the release version to input in release-start command.
 * Release version logic is simple - it is current version without '-SNAPSHOT'
 * @param currentVersion
 * @return
 */
def getReleaseVersion(String currentVersion) {
    echo "Calculating next release version from current version ${currentVersion}"
    for (int i = 0; i < currentVersion.size(); i++) {
        if (currentVersion[i] == "-") {
            return currentVersion.substring(0, i)
        }
    }
}

/**
 * This method calculates the next dev version.
 * The logic is derived from jgit.version file at the root of the codebase.
 *
 * if jgit.version file is not found in the root direction then this function returns the error and pipeline will fail
 * @param currentVersion
 * @return
 */
def getNextDevelopmentVersion(String currentVersion) {
    echo "Calculating next development version from current version ${currentVersion}"
    //0.0.1-SNAPSHOT or 0.1-SNAPSHOT
    def intermediateResult = getReleaseVersion(currentVersion);
    def extractedNumbers = intermediateResult + "."
    //0.0.1. or O.1.
    def values = []
    def start = 0
    def end = 0
    for (element in extractedNumbers) {
        if (element == '.') {
            def additionValue = extractedNumbers.substring(start, end).toInteger()
            values.add(additionValue)
            start = end + 1
        }
        end = end + 1
    }
    try {
        //if jgit.version file is not found in the root direction then this function returns the error
        //and pipeline would fail
        def incrementPattern = readIncrementPattern()
        def releaseVersion = incrementPattern[0]
        def hotFixVersion = incrementPattern[1]
        if (releaseVersion == 0) {
            values[0] = values[0] + 1
        } else if (releaseVersion == 1) {
            values[1] = values[1] + 1
        } else if (releaseVersion == 2) {
            if(values.size() == 3){
                values[2] = values[2] + 1
            }else{
                echo "Wrong release version pattern specified inside jgit.version file."
            }
        }
        if(values.size() == 3){
            return "${values[0]}.${values[1]}.${values[2]}-SNAPSHOT"
        }else{
            return "${values[0]}.${values[1]}-SNAPSHOT"
        }
    }
    catch (exception) {
        echo "${exception}"
    }
    //if jgit.version file is not found in the root direction then this function returns the error
    //and pipeline would fail
}

/**
 * This method reads the jgit.version file in the from the code source directory and calc
 * @return
 */
def readIncrementPattern() {
    //This method reads jgit.version file inside the git repos and takes substring of 5 element after the '=' sign inside that file. adds those values in string format to version array.
    def version = []
    //0th position for release version pattern, 1st postion for hotfix pattern read from file

    // readFile function is form scm git plugin
    def patternValue = readFile(file: 'jgit.version')
    def start = 0
    for (element in patternValue) {
        if (element == '=') {
            def additionValue = patternValue.substring(start + 1, start + 6)
            version.add(additionValue)
        }
        start = start + 1
    }
    finalPositionsOfY = []
    for (element in version) {
        def countPoints = 0
        for (letter in element) {
            if (letter == "y") {
                finalPositionsOfY.add(countPoints)
                break;
            } else if (letter == ".") {
                countPoints = countPoints + 1
            }
        }
    }
    //Finally returns the number of '.' before 'y' so that we can identify the position of it.
    return finalPositionsOfY
}

return this

