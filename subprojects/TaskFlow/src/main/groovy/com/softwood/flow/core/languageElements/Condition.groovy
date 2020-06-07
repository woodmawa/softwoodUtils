package com.softwood.flow.core.languageElements

import groovy.transform.MapConstructor

import java.util.function.Predicate

@MapConstructor
class Condition implements Predicate {

    def defaultItemToTest

    def setItemTotest(item) {
        defaultItemToTest = item
    }

    static Condition newCondition(Closure conditionClosure) {
        Condition condition
        condition = new Condition()
        if (conditionClosure != null)
            condition.dynamicTest = conditionClosure

        condition
    }

    static Condition newCondition(def condArg, Closure conditionClosure) {
        Condition condition
        condition = new Condition()
        if (conditionClosure != null)
            condition.dynamicTest = conditionClosure

        if (condArg)
            condition.defaultItemToTest = condArg
        condition
    }

    /**
     *
     * @param argToTest - value for test
     * @param condClosure - code to run when test() is called
     * @return a Condition
     */

    static Condition flowCondition(argToTest, Closure condClosure) {
        Condition.newCondition(argToTest, condClosure)
    }

    Closure dynamicTest = { false }

    Condition() {}

    boolean test(Object item) {
        defaultItemToTest = item  //cache arg as defaultItemToTest
        def yesNo = dynamicTest(item)
        yesNo
    }

    boolean test() {
        def yesNo = dynamicTest(this.defaultItemToTest)
        yesNo
    }


}
