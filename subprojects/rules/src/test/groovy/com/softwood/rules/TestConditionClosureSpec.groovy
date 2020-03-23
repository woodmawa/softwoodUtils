package com.softwood.rules

import com.softwood.rules.api.Condition
import com.softwood.rules.api.RuleFactory
import com.softwood.rules.core.ConditionClosure
import spock.lang.Specification

class TestConditionClosureSpec extends Specification {

    def "check returned closure from factory " () {
        given:
        Closure input = {it == "william"}
        ConditionClosure condition = RuleFactory.newCondition(RuleFactory.ConditionType.Closure,  [name: "c1#", description : "my first condition"], input)

        expect:
        condition.hasProperty("name")
        condition.owner.is input
        condition.delegate.is input
        condition.thisObject.is input

        //have to use getName as Closure implements getProperty, and defaults to getPropertyOwnerFirst(property) and will ignore any added properties you provide
        condition.name == "c1#"
        condition.description == "my first condition"
        (condition.lowerLimit).is Optional.ofNullable(null)
        (condition.upperLimit).is Optional.ofNullable(null)
        (condition.measure).is Optional.ofNullable(null)

    }

    def "check returned closure from factory supports right shift and left shift chaining " () {
        given:
        ConditionClosure addOne = RuleFactory.newCondition(RuleFactory.ConditionType.Closure,  [name: "c1#", description : "my first condition"]) {it +1 }
        ConditionClosure timesTwo = RuleFactory.newCondition(RuleFactory.ConditionType.Closure,  [name: "c1#", description : "my first condition"]) {it * 2 }


        expect:
        def ls_result = addOne
        def rs_result

        //have to use getName as Closure implements getProperty, and defaults to getPropertyOwnerFirst(property) and will ignore any added properties you provide
        condition.name == "c1#"
        condition.description == "my first condition"
        (condition.lowerLimit).is Optional.ofNullable(null)
        (condition.upperLimit).is Optional.ofNullable(null)
        (condition.measure).is Optional.ofNullable(null)

    }
    def "test BasicCondition with updated dynamicTest" () {
        given: "two conditions that take an arg as input, test equality in the closure"
        String param = "william"
        //create a BasicCondition directly, however default map constructor is looking at public fields not  get/setters, so first create
        //without the explicit use of setter would fail, unless dynamicTest is made public
        Condition condition1 = RuleFactory.newCondition(RuleFactory.ConditionType.Closure,  [name: "c1#", description : "my first condition"]){it == "william"}
        Condition newCondition = condition1.setConditionTest ( {it == "william"} )
        //create a BasicCondition via the RuleFactory, you can pass the condition as a map constructor entry or as closure
        Condition condition2 = RuleFactory.newCondition(RuleFactory.ConditionType.Closure, [name: "c2#", description : "my second condition"]){it == "william"}

        when:
        def  result1 = condition1.test(param)
        def  resultNew = newCondition.test(param)

        def  result2 = condition2.test(param)
        def same = newCondition.is condition1

        then:
        condition1.getName()  == "c1#"
        condition1.getDescription() == "my first condition"
        result1 == true



        and:
        !same
        newCondition.getName()  == "c1#"
        newCondition.getDescription() == "my first condition"
        result1 == true


        and:
        condition2.getName() == "c2#"
        condition2.getDescription() == "my second condition"
        result2 == true
    }

    def "test logical and/or of conditions"() {
        given: "two conditions with no args "
        Condition condition1 = RuleFactory.newCondition(RuleFactory.ConditionType.Closure, [name: "c1#", description : "my first condition"]){5 < it}
        Condition condition2 = RuleFactory.newCondition(RuleFactory.ConditionType.Closure, [name: "c2#", description : "my second condition"]){it < 10 }
        Condition con1OrCon2 = condition1 | condition2
        Condition con1AndCon2 = condition1 & condition2

        when: "we test conditions, and a combination AND and an OR "
        def  result1 = condition1.test(7)
        def  result2 = condition2.test(7)
        def  res1OrRes2 = con1OrCon2.test(7)
        def  res1AndRes2 = con1AndCon2.test(7)

        then:
        condition1.getName() == "c1#"
        condition1.getDescription() == "my first condition"
        result1 == true

        and:
        condition2.getName() == "c2#"
        condition2.getDescription() == "my second condition"
        result2 == true

        and:
        con1OrCon2.getName() == "(c1# | c2#)"
        con1OrCon2.getDescription() == "logical OR"
        res1OrRes2 == true
        res1AndRes2 == true
    }

    def "test a condition where an explicit upper and lower bound have been set "(){
        given: "a conditions set with upper and lower bounds, a measure set but not used   "
        //with ConditionClosure i cant figure out a way to set the delagete to refer to itself  so cant resolve lowerLimit or upperLimit tn this case
        Closure testClos = {it >= lowerLimit && it < upperLimit}
        ConditionClosure condition = RuleFactory.newCondition(RuleFactory.ConditionType.Closure, [name: "arg is between 5 and 10", description: "my first condition", upperLimit:10, lowerLimit:5, measure:8]){it >= 5 && it < 10}
        condition

        when : "we run the test - with parameter in range and out of range"
        def result1 = condition.test (7)
        def result2 = condition.test (12)

        then :
        result1 == true
        result2 == false
    }
}
