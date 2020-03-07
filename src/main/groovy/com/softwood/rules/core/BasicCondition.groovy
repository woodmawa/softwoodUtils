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

    def lowerLimit  = 0
    def upperLimit = 0
    def measure = 0
    String name = "unnamed"
    String description = "unnamed"

    protected Closure dynamicTest = {fact -> println "default condition evaluated $fact, returning false"; return false}

    //this will NOT be called by default map constructor when creating BasicCondition -
    //the groovy logic directly tries to find public attribute - but this is a method
    void setConditionTest (Closure test) {
        assert test
        dynamicTest =  test
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
            return false
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