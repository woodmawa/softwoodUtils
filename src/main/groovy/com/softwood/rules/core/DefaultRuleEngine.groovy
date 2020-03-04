package com.softwood.rules.core

import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule
import com.softwood.rules.api.RuleEngine
import com.softwood.rules.api.RuleSet

/**
 * default Rule engine and takes all the bindable rulelisteners, ruleEngineListeners
 * from the abstract base class
 */

class DefaultRuleEngine extends AbstractRuleEngine implements RuleEngine {

    Collection<Boolean> check(Facts facts, RuleSet rules, arg = null) {
        assert rules, facts
        println "with rules $rules, size ${rules.size()}"
        Collection<Boolean> results = rules.iterator().collect { Rule rule ->
            println "rule '$rule.name' evaluate facts $facts"

            ruleListeners.each {it.beforeEvaluate(rule, facts)}

            boolean res = rule.evaluate (facts, arg)
            ruleListeners.each {it.afterEvaluate(rule, facts, res)}

        }
        results
    }

   def run (Facts facts, RuleSet rules, arg = null) {
       assert rules, facts

       def prioritySortedRules = rules.sort {rule1, rule2 -> rule1.priority <=> rule2.priority}
       Collection<Object> results = prioritySortedRules.iterator().collect { Rule rule ->

           def result
           ruleListeners.each {it.beforeExecute(rule, facts)}
           try {
               result = rule.execute (facts, arg)
               ruleListeners.each {it.onSuccess(rule, facts)}
           } catch  (Exception e) {
               ruleListeners.each { it.onError (rule, facts, e) }
           }
           result
       }
       results
   }


    /**
     * these pair allow rule engine to process facts against a single rule
     * @param facts
     * @param rule
     * @param arg - this will be passed as context data to any rule.action when executing the action closure
     * @return
     */
    boolean check(Facts facts, Rule rule, arg = null) {
        assert rule, facts

        rule.evaluate(facts, arg)

    }

    def run (Facts facts, Rule rule, arg = null) {
        assert rule, facts

        try {
            rule.execute (facts, arg)
            ruleListeners.each {it.onSuccess(rule, facts)}
        } catch  (Exception e) {
            ruleListeners.each { it.onError (rule, facts, e) }
        }
    }

}
