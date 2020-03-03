package com.softwood.rules.scripts

import com.softwood.game.Player
import com.softwood.game.Sensor
import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule
import com.softwood.rules.api.RuleEngine
import com.softwood.rules.api.RuleSet
import com.softwood.rules.core.BasicAction
import com.softwood.rules.core.BasicRule
import com.softwood.rules.core.DefaultRuleEngine


Player toby = new Player (name:"tobias", attributes:["energyLevel":100, "hasSword":false, "newbie":true])
Sensor sensor = new Sensor (name:"energyLevel", owner:toby)


Facts facts = toby.worldState
println facts

Rule rule = new BasicRule (name:"isNewbie", description:"is this a new player",
            action: new BasicAction (name:"print value", action: { fact -> println "$fact.name : $fact.value"}) )

RuleSet rules = new RuleSet(name: "first rule set")
rules.register(rule)

RuleEngine rs = new DefaultRuleEngine()

rs.run(facts, rules)

