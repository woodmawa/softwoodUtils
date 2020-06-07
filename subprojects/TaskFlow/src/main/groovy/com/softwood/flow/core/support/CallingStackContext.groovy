package com.softwood.flow.core.support


import java.util.stream.Collectors
import java.util.stream.Stream

class StackFrameContextInfo {
    String methodType
    int line
    String method
    String className
    List closureList
    boolean callingContextIsClosure
    String description

    String toString() {
        description
    }
}

class CallingStackContext {

    static List<String> fetchLines(Stream<StackWalker.StackFrame> stream) {
        List<StackFrameContextInfo> callingStackList = stream
        //filter for doCall at line equals -1
                .filter { stackFrame ->
                    //if line number is -1 then its the method that calls the stackTrace
                    !stackFrame.getClassName().contains('groovy') && stackFrame.getLineNumber() != -1
                }
                .skip(1)
                .map { stackFrame ->

                    def methodName = stackFrame.getMethodName()
                    def line = stackFrame.getLineNumber()

                    def calleeIsGenerated = false
                    if (stackFrame.getClassName().contains(/$/)) {
                        calleeIsGenerated = true
                    }

                    def containsClos = stackFrame.getClassName().contains('closure')
                    def isDoCall = methodName == 'doCall'
                    def isClosure = containsClos && isDoCall
                    def clazzParts = stackFrame.getClassName().split('[$]')
                    def clazz = clazzParts[0]
                    def clazzClosureParts
                    if (clazzParts.size() > 1)
                        clazzClosureParts = clazzParts[(1..clazzParts.size() - 1)].collect { it[0] == '_' ? it.substring(1) : it }
                    /*if (clazzParts[1][0] == '_')
                        clazzInst = clazzParts[1].substring(1)
                    else
                        clazzInst = clazzParts[1]*/

                    def type = calleeIsGenerated ? 'generated' : 'native'
                    def rets = "class: '${stackFrame.getClassName()}' of type ${clazz} ${clazzClosureParts == null ? '' : 'inst ' + clazzClosureParts},  invoked [$type${isClosure ? ' type of closure' : ""}] method: [$methodName] at  line:$line"
                    StackFrameContextInfo info = new StackFrameContextInfo()
                    info.with {
                        methodType = type
                        line = line
                        method = methodName
                        className = clazzParts[0]
                        List closureList = clazzClosureParts
                        callingContextIsClosure = isClosure
                        description = rets
                    }
                    info
                }
                .collect(Collectors.toList());

        return callingStackList
    }


    static List<StackFrameContextInfo> getContext() {

        List<StackFrameContextInfo> frames = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(this::fetchLines)

    }

}
