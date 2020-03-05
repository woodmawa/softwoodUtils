package com.softwood.rules.core

import com.softwood.rules.api.Condition
import com.softwood.rules.api.Fact
import groovy.transform.ToString

import java.util.function.Predicate

@ToString
class BasicCondition implements Predicate, Condition {

    def lowerLimit  = 0
    def upperLimit = 0
    def measure = 0
    String name = "unnamed"
    String description = "unnamed"

    private Closure dynamicTest = {fact -> println "default condition evaluated $fact.name, with value $fact.value, returning false"; return false}

    void setTest (Closure test) {
        assert test
        dynamicTest =  test
    }

    boolean test (Fact fact = null) {
        if (fact) {
               dynamicTest (fact)
         }
        else
            return false
    }

     boolean test(Object o) {
        return dynamicTest (o)
    }

    Predicate and(Predicate other) {
        return super.and(other)
    }

     Predicate negate() {
        return super.negate()
    }

    Predicate or(Predicate other) {
        return super.or(other)
    }

}