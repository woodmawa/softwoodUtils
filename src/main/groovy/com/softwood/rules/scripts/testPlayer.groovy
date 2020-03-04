package com.softwood.rules.scripts

import com.softwood.game.Player
import com.softwood.game.Sensor
import com.softwood.game.SensorArray
import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule
import com.softwood.rules.api.RuleEngine
import com.softwood.rules.api.RuleSet
import com.softwood.rules.core.BasicAction
import com.softwood.rules.core.BasicFact
import com.softwood.rules.core.BasicRule
import com.softwood.rules.core.DefaultRuleEngine

//create player with some attributes
Player toby = new Player (name:"tobias", attributes:["energyLevel":100, "hasSword":false, "isNewbie":true])

SensorArray sa = new SensorArray()

sa << new Sensor (name:"energyLevel") << new Sensor (name:"hasSword")
sa << new Sensor (name:"isNewbie")
assert sa.sensors.size() == 3

for (s in sa.sensors) {
    println "sense for '$s.name'"
}

Facts facts = sa.getPlayerWorldState(toby)

println facts
assert facts.size() == 3

Rule rule = new BasicRule (name:"isNewbie", description:"is this a new player",
            action: new BasicAction (name:"print value", action: { player -> println "'isNewbie' : ${player.attributes.isNewbie} "; 'success'}) )

rule.postActionEffects << {Player player-> player.applyAttributeStateUpdates([isNewbie:false])}
//give him a sword as well as you know he wants one...
rule.postActionEffects << {Player player-> player.applyAttributeStateUpdates([hasSword:true, energyLevel: 98])}


RuleSet rules = new RuleSet(name: "first rule set")
rules.register(rule)

RuleEngine rs = new DefaultRuleEngine()

//pass the player, toby, as the context to be passed to each rule.action invoked
println rs.run(facts, rules, toby)

println "toby isNewbie now : " + toby.attributes.isNewbie
println toby.attributeList

