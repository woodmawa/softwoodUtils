package com.softwood.rules.core

import com.softwood.rules.api.Condition
import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j
import org.codehaus.groovy.reflection.stdclasses.CachedClosureClass

@MapConstructor
@Slf4j
class ConditionClosure extends Closure<Boolean> implements Condition {

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
        //final CachedClosureClass cachedClass = (CachedClosureClass) ReflectionCache.getCachedClass(getClass());
        //cachedClass
    }

    ConditionClosure(Object owner) {
        super(owner, null)
    }

    def doCall( args) {
        def result = ((Closure) getOwner()).call(args)
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

    Condition and (Condition other) {
        Closure combined = {this.test(it) && other.test(it)}
        ConditionClosure condition =  new ConditionClosure (name: "($name & $other.name)", description: "logical AND")
        condition.conditionTest = combined
        condition
    }

    Condition negate(param =null) {
        return !test (param)
    }

    Condition or (Condition other) {
        //return super.or(other)
        Closure combined = {this.test(it) || other.test(it)}
        ConditionClosure condition =  new ConditionClosure (name: "($name | $other.name)", description: "logical OR")
        condition.conditionTest = combined
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