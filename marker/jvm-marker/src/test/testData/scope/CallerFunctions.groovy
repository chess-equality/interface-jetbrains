class CallerFunctions {
    private void callerFunction() {
        directCalledFunction()
    }

    private void directCalledFunction() {
        indirectCalledFunction()
    }

    private void indirectCalledFunction() {
        println(true)
    }
}