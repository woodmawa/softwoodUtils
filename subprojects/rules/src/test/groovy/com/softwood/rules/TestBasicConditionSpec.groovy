package com.softwood.rules

import com.softwood.rules.api.Condition
import com.softwood.rules.api.RuleFactory
import com.softwood.rules.core.BasicCondition
import spock.lang.Specification

class TestBasicConditionSpec extends Specification {

    def "test BasicCondition with updated dynamicTest"() {
        given: "two conditions that take an arg as input, test equality in the closure"
        String param = "william"
        //create a BasicCondition directly, however default map constructor is looking at public fields not  get/setters, so first create
        //without the explicit use of setter would fail, unless dynamicTest is made public
        Closure inputClos = { it == "william" }
        Condition condition1 = new BasicCondition(name: "c1#", description: "my first condition", dynamicTest: inputClos)
        condition1.conditionTest = { it == "william" }
        //create a BasicCondition via the RuleFactory, you can pass the condition as a map constructor entry or as closure
        Condition condition2 = RuleFactory.newCondition(name: "c2#", description: "my second condition") { it == "william" }

        when:
        def result1 = condition1.test(param)
        def result2 = condition2.test(param)

        then:
        condition1.name == "c1#"
        condition1.description == "my first condition"
        result1 == true

        and:
        condition2.name == "c2#"
        condition2.description == "my second condition"
        result2 == true
    }


    def "test that condition internal closure delegate is set to #this so that it can use the lower/upperLimit and threshold values in logical and/or of conditions"() {
        given: "two conditions with no args "
        Closure inputClos = { lowerLimit < it }
        Condition condition1 = RuleFactory.newCondition(name: "c1#", description: "my first condition", lowerLimit: 5, inputClos)
        Condition condition2 = RuleFactory.newCondition(name: "c2#", description: "my second condition", upperLimit: 10) { it < upperLimit }
        Condition con1OrCon2 = condition1 | condition2
        Condition con1AndCon2 = condition1 & condition2

        when: "we test conditions, and a combination AND and an OR "
        def result1 = condition1.test(4)
        def result2 = condition2.test(7)
        def res1OrRes2 = con1OrCon2.test(7)
        def res1AndRes2 = con1AndCon2.test(4)

        then:
        condition1.name == "c1#"
        condition1.description == "my first condition"
        result1 == false

        and:
        condition2.name == "c2#"
        condition2.description == "my second condition"
        result2 == true

        and:
        con1OrCon2.name == "(c1# | c2#)"
        con1OrCon2.description == "logical OR"
        res1OrRes2 == true
        res1AndRes2 == false
    }

    def "test a condition where an explicit upper and lower bound have been set "() {
        given: "a conditions set with upper and lower bounds, a measure set but not used   "
        Condition condition = RuleFactory.newCondition(name: "arg is between 5 and 10", description: "my first condition", upperLimit: 10, lowerLimit: 5, measure: 8) { it >= lowerLimit && it < upperLimit }

        when: "we run the test - with parameter in range and out of range"
        def result1 = condition.test(7)
        def result2 = condition.test(12)

        then:
        result1 == true
        result2 == false
    }
}
