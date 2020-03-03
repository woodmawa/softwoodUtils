package com.softwood.rules.scripts

import com.softwood.rules.api.Condition
import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts
import com.softwood.rules.api.RuleEngine
import com.softwood.rules.api.Rules
import com.softwood.rules.core.BasicAction
import com.softwood.rules.core.BasicCondition
import com.softwood.rules.core.DefaultRuleEngine
import com.softwood.rules.core.DeprecateBasicFact
import com.softwood.rules.core.BasicRule

def rule = new BasicRule (name:"rule#1", description: "first rule", priority: 1)
//assign action to the rule
rule.action = new BasicAction (name:"action#1", description:"some action", action: {println "did rule action #1"; "act#1"})

Rules rules = new Rules()

assert rules.size() == 0


//register rule1
rules.register(rule)


//create another rule this time with a precondition,
def rule2 = new BasicRule (name:"rule#2", description: "second rule", priority: 0)
rule2.action = new BasicAction (name:"act#2", description:"another action", action: {println "did rule action #2"; "act#2"})
rule2.action << ["some state": "try this for size"]  //add some state data to the action
println "rule2 now has state with ${rule2.action.stateData}"

Condition c = {println "it: $it of type ${it.getClass()}, test it.name == 'act#2' ";  it.name == "act#2"}
def testres = c.test (rule2)
rule2.preConditions << {if (it.name == "sky")
                            it.value == "isBlue-er"
                        else
                            false} as Condition   //takes the closure and coerces it to Condition

//register rule1
rules.register(rule2)

println "rules contains ${rules.size()} rules "

//now create some facts
Facts facts = new Facts(name:"wills facts", description:"starter for 10")

//Fact fact= new DeprecateBasicFact (name:"sky", value:"isBlue")
facts << ["sky": "isBlue"] <<  ["isWintery":true]

//fact = new DeprecateBasicFact()
//fact.setEntry(name:"sky", value:"isBlue")


assert facts.size() == 2
assert facts.name == "wills facts"
assert facts.description == "starter for 10"

//rule engine is statless so pass it any facts and any rules you want evaluated
RuleEngine re = new DefaultRuleEngine()

def res = re.run(facts, rules)

println "results of check were : " + res
//Iterable iFacts = facts.iterator()
//println iFacts.size()

//rule.evaluate([])


