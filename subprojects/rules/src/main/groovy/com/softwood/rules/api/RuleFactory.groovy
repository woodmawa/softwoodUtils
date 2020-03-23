package com.softwood.rules.api

import com.softwood.rules.core.BasicAction
import com.softwood.rules.core.BasicCondition
import com.softwood.rules.core.BasicRule
import com.softwood.rules.core.ConditionClosure
import com.softwood.rules.core.DefaultRuleEngine
import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.MethodClosure

import java.lang.reflect.Constructor
import java.util.function.Predicate

/**
 * Rule factory class to abstract away what the core implementation types are
 *
 * returns instance of api interface class for the user to use
 *
 */
//@CompileStatic
class RuleFactory {
    private static Map actionFactory = [(ActionType.Default.toString()): BasicAction]
    private static Map ruleFactory = [(RuleType.Default.toString()): BasicRule]
    private static Map ruleEngineFactory = [(RuleEngineType.Default.toString()): DefaultRuleEngine]
    private static Map<String, Condition> conditionFactory = [(ConditionType.Default.toString()): BasicCondition,
                                           (ConditionType.Closure.toString()) : ConditionClosure]

    static enum ActionType {
        Default
    }

    static enum RuleType {
        Default
    }

    static enum RuleEngineType {
        Default,Inferencing
    }

    static enum ConditionType {
        Default, Closure
    }

    /**
     * we have two implementations of Condition, one as BasicCondition , the other as a ConditionClosure
     * handles the variances based on the runtime type of the predicate
     * @param reqType - Default or Closure
     * @param initMap - initialisation map if provided
     * @param predicate - a class that implements Predicate
     * @return new Condition (either BasicCondition or ConditionClosure type)
     */
    static Condition newCondition (ConditionType reqType, final Map map=null,  final Closure predicate) {

        Map initMap = [:]
        if (map)
            initMap.putAll (map)

        def klazz = conditionFactory.get(reqType.toString())
        Class<Condition> factoryConditionClazz = klazz

        Constructor<Condition> mapConstructor = factoryConditionClazz.getDeclaredConstructor(Map)
        //instance to api interface definition
        Condition condition

        if (reqType == ConditionType.Closure) {
            //If ConditionClosure type requested, build Condition using the static from methods on ConditionClosure
            if (predicate) {
                if (predicate instanceof Predicate)
                    condition = ConditionClosure.from(predicate::test as MethodClosure)
                else
                    condition  = ConditionClosure.from(predicate as Closure)
            }
            else {
                //no predicate passed so look for one in the initMap
                condition = ConditionClosure.from((initMap?.dynamicTest as Predicate)::test ?: {})
            }
         } else if (reqType == ConditionType.Default) {
            if (mapConstructor && initMap)
                condition = mapConstructor.newInstance(initMap)
            else
                condition = factoryConditionClazz.newInstance()
            if (initMap.test) {
                Predicate pred = initMap.test as Predicate
                condition.setConditionTest(pred::test)
            }
            else if (initMap?.dynamicTest) {
                Predicate pred = initMap.dynamicTest as Predicate
                condition.setConditionTest(pred::test)
            }
            //if there is an explicit closure it takes precedence
            if (predicate)
                condition.setConditionTest (predicate)
        }

        String name = (initMap?.name) ?: "anonymous condition"
        condition.setName(name)
        String description = (initMap?.description) ?: "description: anonymous condition"
        condition.setDescription(description)
        condition.setLowerLimit ( (initMap?.lowerLimit) ?: 0 )
        condition.setUpperLimit ( (initMap?.upperLimit) ?: 0 )
        condition.setMeasure ( (initMap?.measure) ?: 0 )

        condition

    }

    /**
     * if untyped use BasicCondition as default, and call the generic factory method
     * this version accepts a java Functional Predicate interface
     * @param initMap
     * @param predicate
     * @return new BasicCondition
     */
    static Condition newCondition (final Map initMap, final Predicate predicate=null) {
        newCondition(ConditionType.Default, initMap, predicate)
    }

    /**
     * Simplist factory, expects an optional initialisation map and a predicate closure
     * @param (optional) initMap, initialisation map including name & description keys
     * @param predicateClos - (one that returns true or false)
     * @return new BasicCondition
     */
    static Condition newCondition (final Map initMap=null, final Closure predicateClos) {
        newCondition(ConditionType.Default, initMap, predicateClos)
    }



    /**
     * create a new action
     * @param type
     * @param initMap
     * @return
     */

    static Action newAction (ActionType type, final Map map) {

        Map initMap = [:]
        if (map)
            initMap.putAll (map)


        def factoryActionClazz = actionFactory.get(type.toString())

        Constructor mapConstructor = factoryActionClazz.getDeclaredConstructor(Map)

        Action newAction
        if (mapConstructor && initMap)
              newAction = mapConstructor.newInstance(initMap)
        else
            //actionFactory.get(type.toString()).getConstructor().newInstance()
            newAction = factoryActionClazz.newInstance()

        newAction.name = (initMap?.name) ?: "anonymous action"
        newAction.description = (initMap?.description) ?: "description: anonymous action, does nothing"
        if (initMap.action)
            newAction.setAction (initMap.action as Closure)
        newAction
    }

    /**
     * if just closure assigned
     * @param initMap
     * @param newAct
     * @return
     */
    static Action newAction (final Map map =null, final Closure newAct) {
        Map initMap = [:]
        if (map)
            initMap.putAll (map)
        if (newAct)
            initMap << [action: newAct]  //add newAct as action for constructor
        newAction (ActionType.Default, initMap)
    }

    static Action newAction (final Map map =null) {
        Map initMap = [:]
        if (map)
            initMap.putAll (map)
        newAction (ActionType.Default, initMap)
    }


    static Rule newRule (RuleType type, final Map map=null) {
        Map initMap = [:]
        if (map)
            initMap.putAll (map)

        Class<Rule> factoryRuleClazz = ruleFactory.get(type.toString())

        Constructor<Rule> mapConstructor = factoryRuleClazz.getDeclaredConstructor(Map)
        if (mapConstructor && initMap)
            return mapConstructor.newInstance(initMap)
        else
            return factoryRuleClazz.newInstance()
    }

    /**
     * if no declared action type assumes the Default - and at present there is only one type coded for.
     *
     * @param initMap
     * @return
     */
    static Rule newRule (Map initMap =null) {
        newRule (RuleType.Default, initMap)
    }

    /*
     * if rule is created with a closure, assume its for the embedded rule.action
     */
    static Rule newRule (Map initMap =null, @DelegatesTo (Action) Closure actionMethod) {
        assert actionMethod
        Rule rule = newRule (RuleType.Default, initMap)
        //if created with a closure create a default Action using the closure and assign to the rule
        (rule as BasicRule).action  = newAction([name:'RuleFactory initialised'], actionMethod)
        rule

    }

    /*
     * create a new rule engine instance
     */
    static RuleEngine newRuleEngine (RuleEngineType type, Map initMap=null) {

        def factoryRuleEngineClazz = ruleEngineFactory.get(type.toString())

        /*
         * cant call getDeclaredConstructor(Map) if theres only default constructor, throws an exception!
         * so get list of constructors - assumed there be one
         * check parameterTypes for that constructor
         * if that contains Map go find explicitly  - else just create using default constructor
         */
        Constructor[] cons = factoryRuleEngineClazz.constructors
        Class[] cTypes = cons[0].parameterTypes
        if (cTypes.contains(Map)){
            Constructor mapConstructor = factoryRuleEngineClazz.getDeclaredConstructor(Map)
            if (mapConstructor && initMap)
                return mapConstructor.newInstance(initMap)
        }
        else
            return factoryRuleEngineClazz.newInstance()

    }

    static RuleEngine newRuleEngine (Map initMap =null) {
        newRuleEngine (RuleEngineType.Default, initMap)
    }

/*    static Condition newCondition (Map initMap = null) {
        Condition condition = (initMap) ? new BasicCondition(initMap) : new BasicCondition()
        if (initMap?.dynamicTest)
            condition.conditionTest = initMap.dynamicTest as Closure
        if (initMap?.conditionTest)
            condition.conditionTest = initMap.conditionTest as Closure
        condition
    }

    static Condition newCondition (Map initMap = null, Closure test) {
        if (initMap == null)
            initMap = [name: 'Anonymous Condition']
        if (test) {
            initMap << [conditionTest: test]
        }
        Condition condition = (initMap) ? new BasicCondition(initMap) : new BasicCondition()
        if (initMap?.dynamicTest)
            condition.conditionTest = initMap.dynamicTest as Closure
        if (initMap?.conditionTest)
            condition.conditionTest = initMap.conditionTest as Closure
        condition
    }
*/

    static RuleSet newRuleSet (Map initMap = null) {
        if (initMap == null)
            initMap = [name: 'Anonymous RuleSet']

        RuleSet ruleSet = (initMap) ? new RuleSet(initMap) : new RuleSet()
        ruleSet
    }
}
