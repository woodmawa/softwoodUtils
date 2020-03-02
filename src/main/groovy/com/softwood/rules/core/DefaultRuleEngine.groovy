package com.softwood.rules.core

import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule
import com.softwood.rules.api.RuleEngine
import com.softwood.rules.api.Rules
import groovy.beans.Bindable

/**
 * default Rule engine and takes all the bindable rulelisteners, ruleEngineListeners
 * from the abstract base class
 */

class DefaultRuleEngine extends AbstractRuleEngine implements RuleEngine {

    Collection<Boolean> check(Facts facts, Rules rules) {
        assert rules, facts
        println "with rules $rules, size ${rules.size()}"
        Collection<Boolean> results = rules.iterator().collect { Rule rule ->
            println "rule '$rule.name' evaluate facts $facts"

            rule.evaluate(facts)
        }
        results
    }

   def run (Facts facts, Rules rules) {
       assert rules, facts
       Collection<Object> results = rules.iterator().collect { Rule rule ->
           rule.execute (facts)
       }
       results
   }
}
