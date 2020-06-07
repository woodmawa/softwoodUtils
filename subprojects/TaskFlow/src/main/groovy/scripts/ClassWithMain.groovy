package scripts

import com.softwood.flow.core.support.CallingStackContext
import groovy.transform.MapConstructor

import java.util.stream.Collectors
import java.util.stream.Stream

class ClassWithMain {

    @MapConstructor
    class CallingContextInfo {
        String methodType
        int line
        String method
        String className
        boolean callingContextIsClosure
        String description
    }

    static List<String> fetchLines(Stream<StackWalker.StackFrame> stream) {
        List<String> lines = stream
        //filter for doCall at line equals -1
                .filter { stackFrame ->
                    //if line number is -1 then its the method that calls the stackTrace
                    !stackFrame.getClassName().contains('groovy') && stackFrame.getLineNumber() != -1
                }
                .map { stackFrame ->

                    def method = stackFrame.getMethodName()
                    def line = stackFrame.getLineNumber()

                    def calleeIsGenerated = false
                    if (stackFrame.getClassName().contains(/$/)) {
                        calleeIsGenerated = true
                    }

                    def containsClos = stackFrame.getClassName().contains('closure')
                    def isDoCall = method == 'doCall'
                    def isClosure = containsClos && isDoCall
                    def clazzParts = stackFrame.getClassName().split('[$]')
                    def clazz = clazzParts[0]
                    def clazzInst
                    if (clazzParts.size() > 1)
                        if (clazzParts[1][0] == '_')
                            clazzInst = clazzParts[1].substring(1)
                        else
                            clazzInst = clazzParts[1]

                    def type = calleeIsGenerated ? 'generated' : 'native'
                    def rets = "class: '${stackFrame.getClassName()}' of type ${clazz} inst $clazzInst,  invoked [$type${isClosure ? ' type of closure' : ""}] method: [$method] at  line:$line"
                    CallingContextInfo info = new CallingContextInfo(methodType: type,
                            line: line,
                            method: method,
                            className: clazzParts[0],
                            callingContextIsClosure: isClosure,
                            description: rets)
                    rets
                }
                .collect(Collectors.toList());
        return lines;
    }


    static main(args) {

        ClassWithMain inst = new ClassWithMain()
        println "via sub() to myClos () call "
        inst.sub().each { line ->
            println "$line"
        }

        println "\nvia closure  call "
        inst.myFirstClos().each { line ->
            println "$line"
        }

    }

    def sub() {
        myFunc()
    }

    def myFunc() {
        //List<String> lines = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(this::fetchLines);
        List contextInfo = CallingStackContext.getContext()
        contextInfo

    }


    Closure myFirstClos = {
        mySecondClos()
    }
    Closure mySecondClos = {
        myFunc()
    }
}
