package com.softwood.rules.api

import com.softwood.rules.core.BasicAction
import com.softwood.rules.core.BasicCondition
import com.softwood.rules.core.BasicRule
import com.softwood.rules.core.DefaultRuleEngine
import groovy.transform.MapConstructor

import java.lang.reflect.Constructor
import java.lang.reflect.Parameter
import java.lang.reflect.TypeVariable


/**
 * Rule factory class to abstract away what the core implementation types are
 *
 * returns instance of api interface class for the user to use
 *
 */
class RuleFactory {
    private static def actionFactory = [(ActionType.Standard.toString()): BasicAction]
    private static def ruleFactory = [(RuleType.Standard.toString()): BasicRule]
    private static def ruleEngineFactory = [(RuleEngineType.Default.toString()): DefaultRuleEngine]

    static enum ActionType {
        Standard
    }

    static enum RuleType {
        Standard
    }

    static enum RuleEngineType {
        Default,Inferencing
    }

    static Action newAction (ActionType type, Map initMap=null) {

        Class<Rule> factoryActionClazz = actionFactory.get(type.toString())

        Constructor<Action> mapConstructor = factoryActionClazz.getDeclaredConstructor(Map)

        Action action
        if (mapConstructor && initMap)
              action = mapConstructor.newInstance(initMap)
        else
            //actionFactory.get(type.toString()).getConstructor().newInstance()
            action = factoryActionClazz.newInstance()

        action.name = (initMap?.name) ?: "anonymous action"
        action.description = (initMap?.description) ?: "description: anonymous action, does nothing"
        if (initMap.action)
            action.action = initMap.action
        action
    }

    static Action newAction (Map initMap =null) {
        newAction (ActionType.Standard, initMap)
    }

    static Rule newRule (RuleType type, Map initMap=null) {

        Class<Rule> factoryRuleClazz = ruleFactory.get(type.toString())

        Constructor<Rule> mapConstructor = factoryRuleClazz.getDeclaredConstructor(Map)
        if (mapConstructor && initMap)
            return mapConstructor.newInstance(initMap)
        else
            return factoryRuleClazz.newInstance()
    }

    static Rule newRule (Map initMap =null) {
        newRule (RuleType.Standard, initMap)
    }

    static RuleEngine newRuleEngine (RuleEngineType type, Map initMap=null) {

        Class<RuleEngine> factoryRuleEngineClazz = ruleEngineFactory.get(type.toString())

        /*
         * cant call getDeclaredConstructor(Map) if theres only default constructor, throws an exception!
         * so get list of constructors - assumed there be one
         * check parameterTypes for that constructor
         * if that contains Map go find explicitly  - else just create using default constructor
         */
        Constructor[] cons = factoryRuleEngineClazz.constructors
        Class[] cTypes = cons[0].parameterTypes
        if (cTypes.contains(Map)){
            Constructor<RuleEngine> mapConstructor = factoryRuleEngineClazz.getDeclaredConstructor(Map)
            if (mapConstructor && initMap)
                return mapConstructor.newInstance(initMap)
        }
        else
            return factoryRuleEngineClazz.newInstance()

    }

    static RuleEngine newRuleEngine (Map initMap =null) {
        newRuleEngine (RuleEngineType.Default, initMap)
    }

    static Condition newCondition (Map initMap = null) {
        Condition condition = (initMap) ? new BasicCondition(initMap) : new BasicCondition()
        if (initMap.dynamicTest)
            condition.conditionTest = initMap.dynamicTest
        if (initMap.conditionTest)
            condition.conditionTest = initMap.conditionTest
        condition
    }

    static Condition newCondition (Map initMap = null, Closure test) {
        if (test)
            if (initMap)
                initMap << [conditionTest:test]

        Condition condition = (initMap) ? new BasicCondition(initMap) : new BasicCondition()
        if (initMap.dynamicTest)
            condition.conditionTest = initMap.dynamicTest
        if (initMap.conditionTest)
            condition.conditionTest = initMap.conditionTest
        condition
    }

}
