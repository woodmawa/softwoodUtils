package com.softwood.rules.core

import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule
import com.softwood.rules.api.RuleEngine
import com.softwood.rules.api.RuleEngineListener
import com.softwood.rules.api.RuleSet
import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j

/**
 * default Rule engine and takes all the bindable rulelisteners, ruleEngineListeners
 * from the abstract base class
 */

@Slf4j
class DefaultRuleEngine extends AbstractRuleEngine implements RuleEngine {

    /**
     *
     * check the facts against each rule and return a collection pf boolean for each rule
     * if There is any null from any listener then the rule processing should be stopped
     * @param facts
     * @param rules
     * @param arg - any data item that you wish the executed rule to receive
     * @return collection of true and false for each rule assesed against the facts
     */
    Collection<Boolean> check(Facts facts, RuleSet rules, arg = null) {
        assert rules, facts
        log.debug("with rules $rules, size ${rules.size()}")

        //if any ruleEnginListeners are set, then process them
        ruleEngineListeners.each {it?.beforeEvaluate(rules, facts)}

        Collection<Boolean> results = rules.iterator().collect { Rule rule ->
            println "rule '$rule.name' evaluate facts $facts"

            def yehNey = ruleListeners.each {it?.beforeEvaluate(rule, facts)}
            if (yehNey.any {it == false}) {
                log.debug( "check facts with rules: One of the RuleListeners returned false, you should stop evaluation of the Rule  ")
                return
            }

            boolean res = rule.evaluate (facts, arg)
            ruleListeners.each {it?.afterEvaluate(rule, facts, res)}

        }
        results
    }

   def run (Facts facts, RuleSet rules, arg = null) {
       assert rules, facts

       def prioritySortedRules = rules.sort {rule1, rule2 -> rule1.priority <=> rule2.priority}
       Collection<Object> resultsList = prioritySortedRules.iterator().collect { Rule rule ->

           def result
           //check if any listeners.beforeExecute want to override the execution of the rule
           if (check (facts, rule, arg)) {
               ruleListeners.each { it?.beforeExecute(rule, facts) }
               try {
                   log.debug("run: executing rule $rule, passing facts <$facts> for preConditions check")
                   result = rule.execute(facts, arg)
                   ruleListeners.each { it?.onSuccess(rule, facts) }
                   result
               } catch (Exception e) {
                   ruleListeners.each { it?.onError(rule, facts, e) }
               }
           }


           result
       }

       //if any ruleEnginListeners are set, then  process them
       ruleEngineListeners.each {it?.afterExecute(rules, facts, resultsList)}

       resultsList
   }


    /**
     * these pair allow rule engine to process facts against a Single  rule, rather an ruleSet
     * @param facts
     * @param rule
     * @param arg - this will be passed as context data to any rule.action when executing the action closure
     * @return
     */
    boolean check(Facts facts, Rule rule, arg = null) {
        assert rule, facts

        //if any ruleEnginListeners are set, then process them
        ruleEngineListeners.each {it?.beforeRuleEvaluate(rule, facts)}

        def yehNey = ruleListeners.each {it?.beforeEvaluate(rule, facts)}
        if (yehNey.any {it == false}) {
            log.debug( "check facts with individual rule $rule: One of the RuleListeners returned false, stopping evaluation of the Rule  ")
            return false
        }

        boolean result = rule.evaluate(facts, arg)
        ruleListeners.each {it?.afterEvaluate(rule, facts, result)}
        result

    }

    def run (Facts facts, Rule rule, arg = null) {
        assert rule, facts

        def result
        ruleListeners.each {it?.beforeExecute(rule, facts)}
        try {
            //if check returns false - then the rule should not be run
            if (check (facts, rule, arg)) {
                log.debug ("run: execute the the rule with facts <$facts>, and arg  <$arg>")
                result = rule.execute(facts, arg)
                ruleListeners.each {it?.onSuccess(rule, facts)}
            }
        } catch  (Exception e) {
            ruleListeners.each { it?.onError (rule, facts, e) }
        }

        //if any ruleEnginListeners are set, then  process them
        ruleEngineListeners.each {it?.afterRuleExecute(rule, facts, result)}

        result
    }

}
