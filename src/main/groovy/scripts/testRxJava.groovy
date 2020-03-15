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
cons.accept(10)

//force SAM coercion
Function f = {println it + " william"; it} as Function
println "for f('hi') returned > " + f('hi')


Function func = FunctionalClosure.functionFrom ({println it; "function done"})

println func ("william2")
Consumer cons2 = FunctionalClosure.consumerFrom { num -> println num}

MethodClosure mc = FunctionalClosure.asMethodClosure {println it}


MethodClosure sout = System.out::println
sout ("invoke System.out.println as a MethodClosure :")

//get a method closure from an ordinary closure
def methClos = {println it}::call
 methClos ("try invoke methodClosure() from a closure ")

//SAM coercion for closure to Function
Function doubler = {num -> print "number to double is $num > "; 2*num} as Function
println doubler.apply (10)

Flowable flowl = Flowable.fromIterable([1,2,3])
flowl.map{num -> num*2}.subscribe({println it} )


Flowable flow2 = Flowable.fromIterable([1,2,3])
flow2.subscribe (cons)