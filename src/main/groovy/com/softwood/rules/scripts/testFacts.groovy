package com.softwood.rules.scripts


import com.softwood.rules.api.Facts

//Facts has a hashMap as delegate so gets all map features as well
Facts facts = new Facts (name:"myfacts", description:"some facts")

println "facts instance called : " + facts.name

//facts.add (["sky": "isBlue"])
facts << ["sky": "isBlue"] << ["high":"noon"]
facts.putAll (["what": 0])

List list = facts.asList()
println list

Iterator iterable = facts.iterator()

// i.each {println "$it :  ${it.getClass()}" }
for (Map.Entry  node in iterable) {
  println node.key + "  : " + node.value
}



String f1 = facts.getFact("sky")
int num = facts.getFact ("what")
println "<--->"
println f1
println num
println facts.high


