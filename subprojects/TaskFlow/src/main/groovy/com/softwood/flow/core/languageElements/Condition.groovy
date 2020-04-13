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

    static Condition newCondition(itemToTest, Closure conditionClosure) {
        Condition condition
        condition = new Condition ()
        if (conditionClosure != null)
            condition.dynamicTest =  conditionClosure

        if (itemToTest)
            condition.defaultItemToTest = itemToTest
        condition
    }

    //Made this a closure so that the whens delegate can be set, and we can resolve newInClosure
    static def when = {Condition condition, itemToTest = null,  Closure toDo ->

        //try and resolve newInClosure list on FlowContext delegate
        List nicl
        if (delegate.hasProperty('newInClosure')) {
            nicl = delegate?.newInClosure
            nicl.add (condition)
        }
        if (itemTotest != null){
            if (condition && condition.test (itemToTest)) {
                toDo (args)
            } else {
                if (condition && condition.test ())
                toDo (args)
            }
        }
        else
            false       //fail as default

    }

    /*static boolean when ( Condition condition, itemToTest,  Closure toDo) {

        //try and resolve newInClosure list on FlowContext delegate
        if (condition && condition.test (itemToTest)) {
            toDo (itemToTest)
        }  else
            false       //fail as default
    }*/

    static def when (Predicate predicate, itemToTest,  Closure toDo) {
        if (predicate && predicate.test (itemToTest)) {
            toDo ()
    }

    Closure dynamicTest = { false}

    Condition () {}

    boolean test(Object itemToTest) {
        def yesNo =  dynamicTest (itemToTest)
        yesNo
    }

    boolean test() {
        def yesNo =  dynamicTest (this.defaultItemToTest)
        yesNo
    }

}
