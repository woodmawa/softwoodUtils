package com.softwood.rules.core

import com.softwood.rules.api.Condition
import com.softwood.rules.api.Fact
import groovy.transform.MapConstructor
import groovy.transform.ToString
import groovy.util.logging.Slf4j

import java.util.function.Predicate

@MapConstructor
@Slf4j
class BasicCondition implements Condition {

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

    Closure dynamicTest = {fact -> println "default condition evaluated $fact, returning false"; return false}

    //this setter will NOT be called by default map constructor when creating BasicCondition -
    //the groovy logic directly tries to find public attribute - but this is a method so its not called
    void setConditionTest (Closure test) {
        assert test
        dynamicTest =  test
        dynamicTest.resolveStrategy = Closure.DELEGATE_FIRST
        dynamicTest.delegate = this
    }

    Closure getConditionTest () {
        dynamicTest
    }

    boolean test (fact = null) {
        log.debug "evaluated test with <$fact> as input to the test"
        if (fact) {
            if (dynamicTest.maximumNumberOfParameters > 0)
               return dynamicTest (fact)
            else
                return dynamicTest()
         }
        else
            return dynamicTest()    //just invoke the no args test
    }

    Condition and (Condition other) {
        Closure combined = {this.test(it) && other.test(it)}
        BasicCondition condition =  new BasicCondition (name: "($name & $other.name)", description: "logical AND")
        condition.conditionTest = combined
        condition
    }

    Condition negate(param =null) {
        return !test (param)
    }

    Condition or (Condition other) {
        //return super.or(other)
        Closure combined = {this.test(it) || other.test(it)}
        BasicCondition condition =  new BasicCondition (name: "($name | $other.name)", description: "logical OR")
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
                this.test(param)
            else
                this.test()
        else
            this //just use groovy truth here - return this condition, would normally be 'true'
    }

    String toString() {
        "${this.getClass().name} ($name, $description)"
    }
}