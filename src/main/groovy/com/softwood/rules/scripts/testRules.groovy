package com.softwood.rules.scripts

import com.softwood.rules.api.Condition
import com.softwood.rules.api.Facts
import com.softwood.rules.api.RuleEngine
import com.softwood.rules.api.RuleListener
import com.softwood.rules.api.RuleSet
import com.softwood.rules.core.BasicAction
import com.softwood.rules.core.DefaultRuleEngine
import com.softwood.rules.core.BasicRule

import java.beans.PropertyChangeListener

def rule = new BasicRule (name:"rule#1", description: "first rule", priority: 1)
//assign action to the rule
rule.action = new BasicAction (name:"action#1", description:"some action", action: {println "did rule action #1"; "act#1"})

RuleSet rules = new RuleSet()

assert rules.size() == 0


//register rule1
rules.register(rule)


//create another rule this time with a precondition,
def rule2 = new BasicRule (name:"rule#2", description: "second rule", priority: 0)
rule2.action = new BasicAction (name:"act#2", description:"another action", action: {println "did rule action #2"; "act#2"})
rule2.action.stateData << ["some state": "try this for size"]  //add some state data to the action
println "rule2 now has state with ${rule2.action.stateData}"

//Condition c = {println "it: $it of type ${it.getClass()}, test it.name == $it.name "
               //it.value == "isBlue"}
//def testres = c.test (rule2)
rule2.preConditions << {
    def arg = it
    if (it.name == "sky")
                            it.value == "isBlue"
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

Map listener = [afterEvaluate: {lrule, lfacts, lresult -> println "listener: after evaluate rule $lrule with facts $lfacts returned $lresult "},
                 beforeExecute: {lrule, lfacts -> println "listener: before the run the action on $lrule with facts $lfacts"},
                 onSuccess: {lrule, lfacts -> println "listener: succeeded to run the action on $lrule with facts $lfacts"}
                ]
re.registerRuleListener( listener as RuleListener)

def res = re.run(facts, rules)

println "results of check were : " + res
//Iterable iFacts = facts.iterator()
//println iFacts.size()

//rule.evaluate([])


