package com.softwood.rules

import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule
import com.softwood.rules.api.RuleFactory
import spock.lang.Specification


class TestRuleSpec extends Specification {

    def "build a rule with preconditions and effects, and execute where conditions are met, where they dont"() {
        given: "new rule with preConditions and effects "
            def result, effectResult
            Rule rule = RuleFactory.newRule (name:'rule#1', description:'new rule', priority:10){ result = "$it rule executed"}
            rule.preConditions << RuleFactory.newCondition () {fact -> fact.value > 10 }
            rule.postActionEffects << {effectResult = "done"}
            Facts facts = new Facts() << [age: 57]
            Fact fact = facts.findFact('age')

        when: "try execute the rule and pass arg that will be passed to action closure  "
            def ret = rule.execute(facts, "william's")

         then:"check that external refs have been updated  "
            result ==  "william's rule executed"
            effectResult == "done"
    }

    def "build a rule with no preconditions and an effect, and just execute the action "() {
        given: "new rule with preConditions and effects "
            def result, effectResult
            Rule rule = RuleFactory.newRule (name:'rule#1', description:'new rule', priority:10){ result = "rule executed"}
            rule.postActionEffects << {effectResult = "done"}


        when: "try and evoke the rule directly "
            def ret = rule.justExecute("william", )

         then:" "
         result ==  "rule executed"
         effectResult == "done"
    }
}
