package com.softwood.flow.core.languageElements

import groovy.transform.MapConstructor

import java.util.function.Predicate

@MapConstructor
class Condition implements Predicate {

    def defaultItemToTest

    def setItemTotest (item) {
        defaultItemToTest = item
    }

    static Condition newCondition(Closure conditionClosure) {
        Condition condition
        condition = new Condition ()
        if (conditionClosure != null)
            condition.dynamicTest = conditionClosure

        condition
    }

    static Condition newCondition(condArg, Closure conditionClosure) {
        Condition condition
        condition = new Condition ()
        if (conditionClosure != null)
            condition.dynamicTest =  conditionClosure

        if (condArg)
            condition.defaultItemToTest = condArg
        condition
    }



    /*static boolean when ( Condition condition, itemToTest,  Closure toDo) {

        //try and resolve newInClosure list on FlowContext delegate
        if (condition && condition.test (itemToTest)) {
            toDo (itemToTest)
        }  else
            false       //fail as default
    }*/

    Closure dynamicTest = { false}

    Condition () {}

    boolean test(Object item) {
        def yesNo =  dynamicTest (item)
        yesNo
    }

    boolean test() {
        def yesNo =  dynamicTest (this.defaultItemToTest)
        yesNo
    }



}
