package scripts

import groovy.transform.InheritConstructors

import java.util.concurrent.Callable

@InheritConstructors
class Wclosure extends Closure {

    Wclosure(Closure clos) {
        super(clos.owner, clos.thisObject)
        def d = delegate
        def t = this
        metaClass.setProperty(this, "run", clos::call)
    }

    Wclosure(Callable clos) {
        super(clos, clos)
        def d = delegate
        def t = this
        metaClass.setProperty(this, "run", clos::call)
    }

    Wclosure(Object owner, Closure clos) {
        super(owner, clos.thisObject)
        def d = delegate
        def t = this
        metaClass.setProperty(this, "run", clos::call)
    }

    Callable run

    def doCall(message) {
        if (run)
            run(message)
        else
            println "wClosure: default implementation $message"
    }
}

def w = new Wclosure(this, this)
def w2 = new Wclosure(this, { println "func latest $it" })

w("hi william")
w2("hi marian")

String.metaClass.greet = { mess -> println "added greet, $mess" }

String anys = "will"
anys.greet('will')