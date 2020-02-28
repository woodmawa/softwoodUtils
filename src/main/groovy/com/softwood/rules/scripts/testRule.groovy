package com.softwood.rules.scripts

import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rules
import com.softwood.rules.core.BasicAction
import com.softwood.rules.core.BasicFact
import com.softwood.rules.core.BasicRule

def rule = new BasicRule (name:"rule#1", description: "first rule", priority: 1)
rule.action = new BasicAction (name:"act#1", description:"some action", action: {println "did rule action"})

Rules rules = new Rules()

rules.register(rule)
Facts facts = new Facts()

Fact fact= new BasicFact (key:"sky", value:"isBlue")
facts << fact

//Iterable iFacts = facts.iterator()
//println iFacts.size()

rule.evaluate([])