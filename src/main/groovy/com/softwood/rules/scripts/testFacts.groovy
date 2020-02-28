package com.softwood.rules.scripts

import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts
import com.softwood.rules.core.BasicFact


//Facts has a hashMap as delegate so gets all map features as well
Facts facts = new Facts (name:"myfacts", description:"some facts")

//facts.add (["sky": "isBlue"])
facts << ["sky": "isBlue"] << ["high":"noon"]
facts.putAll (["what": "now"])

List list = facts.asList()
println list

Iterator i = facts.iterator()

// i.each {println "$it :  ${it.getClass()}" }
for (Map.Entry  node in i) {
  println node.key + "  : " + node.value
}


Fact f1 = facts.fact("sky")

println f1

assert f1.size() == 1

def n = f1.name   //this tries to look up the string value as 'key' which has null entry
println n
println "get entry for sky  " + f1.value //this invokes getter - works !