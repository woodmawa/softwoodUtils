package com.softwood.rules.scripts

import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rules
import com.softwood.rules.core.BasicAction
import com.softwood.rules.core.DeprecateBasicFact
import com.softwood.rules.core.BasicRule

def rule = new BasicRule (name:"rule#1", description: "first rule", priority: 1)
rule.action = new BasicAction (name:"act#1", description:"some action", action: {println "did rule action #1"})

Rules rules = new Rules()

assert rules.size() == 0



rules.register(rule)

rule.action = new BasicAction (name:"act#2", description:"some action", action: {println "did rule action #2"})
rules.register(rule)

Facts facts = new Facts()

Fact fact= new DeprecateBasicFact (name:"sky", value:"isBlue")

fact = new DeprecateBasicFact()
fact.setEntry(name:"sky", value:"isBlue")


facts << fact << new DeprecateBasicFact (name:"isWintery", value: true)

assert facts.size() == 2

//Iterable iFacts = facts.iterator()
//println iFacts.size()

rule.evaluate([])


