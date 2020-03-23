package com.softwood.rules.core

import com.softwood.rules.api.Condition
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j

import java.util.function.Predicate

@MapConstructor
@Slf4j
@CompileStatic
class BasicCondition implements Condition {

    /*
     * lowerLimit, upperLimit and measure are values that be for a condition and
     * used in the test closure
     *
     * measure can be set as absolute value to test against
     *
     *
     */
    def lowerLimit  = 0
    def upperLimit = 0
    def measure = 0


    //A condition can have a name, and a description
    final String UNNAMED = "unnamed"
    String name = UNNAMED
    String description = UNNAMED

    //basic version just embeds a closure as proxy to invoke when test() is called - returns false by default
    Closure dynamicTest = {fact -> log.debug "default condition evaluated $fact, returning false"; return false}

    //this setter will NOT be called by default map constructor when creating BasicCondition -
    //the groovy logic directly tries to find public attribute - but this is a method so its not called
    //it generates a new instance of Condition with the new predicate set
    Condition setConditionTest (Closure closurePredicate) {
        assert closurePredicate
        dynamicTest =  closurePredicate.clone() as Closure
        dynamicTest.resolveStrategy = Closure.DELEGATE_FIRST
        dynamicTest.delegate = this
        this
    }

    Condition setConditionTest (Predicate predicate) {
        assert predicate
        dynamicTest =  predicate::test
        dynamicTest.resolveStrategy = Closure.DELEGATE_FIRST
        dynamicTest.delegate = this
        this
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
                return dynamicTest(null)
         }
        else
            return dynamicTest(null)    //just invoke the no args test
    }

    Condition and (Condition other) {
        Closure combined = {test(it) && other.test(it)}
        String combinedName = (name == UNNAMED && other.name == UNNAMED) ? UNNAMED : "($name & $other.name)"
        BasicCondition condition =  new BasicCondition (name: "$combinedName", description: "logical AND")
        condition.setConditionTest (combined as Predicate)
        condition
    }

    Condition negate() {
       BasicCondition condition = this.clone() as BasicCondition
       condition.dynamicTest = {! dynamicTest(it)}
       condition.name = "(Not $name)"

       return condition
    }

    Condition or (Condition other) {
        //return super.or(other)
        Closure combined = {test(it) || other.test(it)}
        String combinedName = (name == UNNAMED && other.name == UNNAMED) ? UNNAMED : "($name | $other.name)"
        BasicCondition condition =  new BasicCondition (name: "$combinedName", description: "logical OR")
        condition.setConditionTest (combined as Predicate)
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