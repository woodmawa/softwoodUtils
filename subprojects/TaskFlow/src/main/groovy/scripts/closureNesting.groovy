package scripts

import java.util.stream.Collectors
import java.lang.StackWalker.StackFrame
import java.util.stream.Stream


public static String getCallerMethodName() {
    return StackWalker.getInstance()
            .walk(s -> s.skip(2).findFirst())
            .get()
            .getMethodName();
}

static List<String> fetchLines(Stream<StackFrame> stream) {
    List<String> lines = stream
    //filter for doCall at line equals -1
            .filter { stackFrame -> !stackFrame.getClassName().contains('groovy') && stackFrame.getLineNumber() == -1 }
            .map { stackFrame ->

                def clazzParts = stackFrame.getClassName().split('[$]')
                def clazz = clazzParts[0]
                def clazzInst
                if (clazzParts.size() > 1)
                    clazzInst = clazzParts[1].substring(1)

                def s = "class: '${stackFrame.getClassName()}' of type ${clazz} inst $clazzInst,  invoked method: ${stackFrame.getMethodName()} at  line:${stackFrame.getLineNumber()}"
                //def s = stackFrame.toString()
                s
            }
            .collect(Collectors.toList());
    return lines;
}

/*def fetch = this::fetchLines
class A {

    def method (fet) {
        List<String> lines = StackWalker.getInstance().walk(fet);
        lines
    }
}

def a = new A()
a.method (fetch)
*/
Closure another = {
    println "inner owner : $owner, with type $owner.class"

    List<String> lines2 = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(this::fetchLines);
    println lines2
    lines2
}

Closure outer = {
    println "outers this : $this, outer owner : $owner, with type $owner.class"

    def st = Thread.currentThread().getStackTrace()
    def caller = getCallerMethodName()

    List<StackFrame> stack = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(s ->
            s.limit(10).collect(Collectors.toList()));

    List<String> lines = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(this::fetchLines);

    Closure inner = {
        println "inner owner : $owner, with type $owner.class"

        List<String> lines2 = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(this::fetchLines);
        lines2

    }
    another()
    inner
}


boolean isClosure(someObject) {
    someObject.hasProperty('owner')
}

//assert isClosure (new A()) == false
//assert isClosure (outer) == true

println "this in script :  $this"
def clos = outer()

