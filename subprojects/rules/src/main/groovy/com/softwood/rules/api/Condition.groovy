package com.softwood.rules.api

import java.util.function.Predicate

interface Condition extends Predicate {
    Condition or(Condition c)

    Condition and(Condition c)
    //Condition negate () default defined in Predicate

    String getName()

    void setName(String name)

    String getDescription()

    void setDescription(String description)

    void setLowerLimit(def lowerBound)

    def getLowerLimit()

    void setUpperLimit(def upperBound)

    def getUpperLimit()

    void setMeasure(def measure)

    def getMeasure()

    Condition setConditionTest(Predicate test)

    Condition setConditionTest(Closure test)

}