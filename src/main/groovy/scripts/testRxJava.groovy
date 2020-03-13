package scripts

import groovyx.runtime.FunctionalClosure
import io.reactivex.rxjava3.core.Flowable

import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.functions.Function
import org.codehaus.groovy.runtime.MethodClosure



//Consumer cons = new FunctionalClosure (this, {println it})
Consumer cons = new FunctionalClosure ()
cons << {println it}

//as this really a closure we can just invoke it
cons("william")

Function func = FunctionalClosure.functionFrom ({println it; "function done"})

println func ("william2")
Consumer cons2 = FunctionalClosure.consumerFrom { num -> println num}

MethodClosure mc = FunctionalClosure.asMethodClosure {println it}

cons.accept(10)


def sout = System.out::println
Flowable pl = Flowable.fromIterable([1,2,3])

//Function pc = {num -> println num} as Function

pl.map{num -> num*2}.subscribe(cons)


