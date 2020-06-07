package com.softwood.rules.scripts

import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule
import com.softwood.rules.api.RuleEngine
import com.softwood.rules.api.RuleFactory
import com.softwood.rules.api.RulesBuilder

def builder = new RulesBuilder()

Closure yourTired = { println "your tired 2" }

Closure effectAction = { println "applying my effect" }
def rules = builder.ruleSet('myRules') {
    rule('myRule', description: 'first rule', priority: 0) {
        preCondition('isTired', description: 'must meet', lowerLimit: -1, upperLimit: 10, test: { fact -> fact.value < 60 })
        action('act#1', description: "do something", stateData: [:], doAction: { println "your tired"; 'success' })
        effect(action: effectAction)
    }
}

rules.each { Rule rule ->
    print "just do the action directly : "
    rule.action.invoke()
}

Facts facts = new Facts()
facts << ['energy': 50]

RuleEngine re = RuleFactory.newRuleEngine([attributes: [oil: 50]])

println "-->now run the ruleEngine<---"
def result = re.run(facts, rules)

println "output from engine run is $result"