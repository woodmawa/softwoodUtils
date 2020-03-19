package com.softwood.rules.core

import com.softwood.rules.api.Condition
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j
import org.codehaus.groovy.reflection.stdclasses.CachedClosureClass
import org.codehaus.groovy.runtime.ComposedClosure

@MapConstructor
@Slf4j
@CompileStatic
class ConditionClosure<V> extends Closure<V> implements Condition {

    /**
     * add static builder to create ConditionClosure from original source closure
     * @param closure
     * @return
     */
    static from (Closure closure) {
        //could clone here
        new ConditionClosure (closure, closure.thisObject)
    }

    /*
     * lowerLimit, upperLimit and measure are values that be for a condition and
     * used in the test closure
     *
     * measure can be set as absolute value to test against
     */
    def lowerLimit  = 0
    def upperLimit = 0
    def measure = 0

    //A condition can have a name, and a description
    String name = "unnamed"
    String description = "unnamed"

    ConditionClosure(Object owner, Object thisObject ) {
        super(owner, thisObject)
    }

    ConditionClosure(Object owner) {
        super(owner, null)
    }

    public  Closure leftShift(final Closure other) {
        ComposedClosure composed =  new ComposedClosure(other, this)
        System.out.println "return composedClosure ${composed.toString()}  from ${other.toString()}"
        composed
    }

    /**
     * implement a doCall on this class. call() on inherited Closure will route to this doCall()
     * @param args
     * @return
     */

    def doCall( args) {
        Closure clos = (Closure) getOwner()
        def result = clos.call(args)
        println "return result as $result"
        result
    }


    boolean test (fact = null) {
        log.debug "evaluated test with <$fact> as input to the test"
        if (fact) {
            if (maximumNumberOfParameters > 0)
               return call (fact)
            else
                return call()
         }
        else
            return call()    //just invoke the no args test
    }

    Condition and (final Condition other) {
        Closure combined = {this.test(it) && other.test(it)}
        ConditionClosure condition =  ConditionClosure<Boolean>.from (combined)
        condition.name = "($name & $other.name)"
        condition.description = "logical AND"
        condition
    }

    Boolean  negate(param =null) {
        return !test (param)
    }

    Condition or (final Condition other) {
        //return super.or(other)
        Closure combined = {this.test(it) || other.test(it)}
        ConditionClosure condition =  ConditionClosure<Boolean>.from (combined)
        condition.name = "($name & $other.name)"
        condition.description = "logical OR"
        condition
    }

    /*
     * if coerced to boolean evaluate the test and return it
     * otherwise just use the default groovy truth for this
     */
    boolean asType (Class clazz, param=null) {
        assert clazz
        if (clazz == Boolean)
            if (param)
                test(param)
            else
                test()
        else
            this //just use groovy truth here - return this condition, would normally be 'true'
    }

    String toString() {
        "${this.getClass().name} ($name, $description)"
    }
}