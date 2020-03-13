package scripts

import io.reactivex.rxjava3.core.Flowable

import io.reactivex.rxjava3.functions.Consumer
import org.codehaus.groovy.runtime.MethodClosure

/**
 * class to wrap a closure and convert it into a RxJava Consumer
 * @param <T>  expected type of the arg that that the closure will be called with
 */
class Processor<T> implements Consumer<T> {
    Closure action
    void accept (T arg) {
        action (arg)
    }

    static <T> Consumer<T>  from (Closure clos ) {
        Processor cons = new Processor()
        cons.action = clos
        cons
    }

    static <T> MethodClosure asMethodClosure (Closure clos ) {
        Processor cons = new Processor()
        cons.action = clos
        cons::accept
    }
}

Consumer cons = new Processor (action : { num -> println num} )
Consumer cons2 = Processor.from { num -> println num}

MethodClosure mc = Processor.asMethodClosure {println it}

cons.accept(10)


def sout = System.out::println
Flowable pl = Flowable.fromIterable([1,2,3])

//Function pc = {num -> println num} as Function

pl.map{num -> num*2}.subscribe(mc)
