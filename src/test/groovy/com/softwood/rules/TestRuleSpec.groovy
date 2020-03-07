package com.softwood.rules

import com.softwood.rules.api.Rule
import com.softwood.rules.api.RuleFactory
import spock.lang.Specification

class TestRuleSpec extends Specification {

    def "build a rule with preconditions and effects, and execute where conditions are met, where they dont"() {
        given: "new rule with preConditions and effects "
            def result, effectResult
            Rule rule = RuleFactory.newRule (name:'rule#1', description:'new rule', priority:10){ result = "rule executed"}
            rule.preConditions << RuleFactory.newCondition () {it > 10 }
            rule.effects << {effectResult = "done"}
            

            then: "try and evoke the rule "
            rule.execute(facts, )
    }
}
