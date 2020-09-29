
def parseInputData(String inputValue) {
    def names = []
    def start = 0
    def end = 0
    def var = inputValue
    for (element in var) {
        if (element == ';') {
            def additionValue = var.substring(start, end)
            names.add(additionValue)
            start = end + 1
        }
        end = end + 1
    }
    return names
}

return this
