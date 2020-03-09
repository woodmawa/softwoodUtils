package com.softwood.rules.scripts

import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule
import com.softwood.rules.api.RuleEngine
import com.softwood.rules.api.RuleFactory
import com.softwood.rules.api.RulesBuilder

def builder = new RulesBuilder ()

Closure yt = {println "your tired 2"}

def rules = builder.ruleSet ('myRules') {
    rule ('myRule', description:'first rule', priority:0) {
        preCondition ('isTired', description:'must meet',lowerLimit:-1, upperLimit:10 , test:{ fact-> fact.value < 60}) /*{ fact-> fact.value < 60}*/
        action ('act#1', description:"do something", stateData:[:], doAction : {println "your tired"}) /*{println "your tired 2"}*/
        effect (action : {println "applying my effect"})
    }
}

rules.each { Rule rule->
    println "just do the action"
    rule.action.doAction()

}
Facts facts = new Facts()
facts << ['energy': 50]

RuleEngine re = RuleFactory.newRuleEngine([attributes:[oil:50]])

re.run (facts, rules)

println rules