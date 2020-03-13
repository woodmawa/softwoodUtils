package scripts

import io.reactivex.rxjava3.core.Flowable

import io.reactivex.rxjava3.functions.Consumer
import org.codehaus.groovy.runtime.MethodClosure

import java.util.function.Function

/**
 * class to wrap a closure and convert it into a RxJava Consumer
 * @param <T>  expected type of the arg that that the closure will be called with
 */
class Consumable<T> implements Consumer<T> {
    Closure action
    void accept (T arg) {
        action (arg)
    }

    static <T> Consumer<T>  from (Closure clos ) {
        Consumable cons = new Consumable()
        cons.action = clos
        cons
    }

    static <T> MethodClosure asMethodClosure (Closure clos ) {
        Consumable cons = new Consumable()
        cons.action = clos
        cons::accept
    }
}

Consumer cons = new Consumable (action : {num -> println num} )
Consumer cons2 = Consumable.from {num -> println num}

MethodClosure mc = Consumable.asMethodClosure {println it}

cons.accept(10)


def sout = System.out::println
Flowable pl = Flowable.fromIterable([1,2,3])

//Function pc = {num -> println num} as Function

pl.map{num -> num*2}.subscribe(mc)
