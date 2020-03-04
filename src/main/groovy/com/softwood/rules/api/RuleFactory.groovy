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
    private static def ruleFactory = [(ActionType.Standard.toString()): BasicRule]
    private static def ruleEngineFactory = [(ActionType.Standard.toString()): DefaultRuleEngine]

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

        action.name = (action.name != "") ?: "anonymous action"
        action.description = (action.description != "") ?: "description: anonymous action, does nothing"
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

        Class<Rule> factoryRuleEngineClazz = ruleEngineFactory.get(type.toString())

        Constructor<Rule> mapConstructor = factoryRuleEngineClazz.getDeclaredConstructor(Map)
        if (mapConstructor && initMap)
            return mapConstructor.newInstance(initMap)
        else
            return factoryRuleEngineClazz.newInstance()

    }

    static RuleEngine newRuleEngine (Map initMap =null) {
        newRuleEngine (RuleEngineType.Default, initMap)
    }

    static Condition newCondition (Map initMap = null) {
        Class<Condition> factoryConditionClazz = (initMap) ? new BasicCondition(initMap) : new BasicCondition()

    }



}
