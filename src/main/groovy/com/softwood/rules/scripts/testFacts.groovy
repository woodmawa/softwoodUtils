package com.softwood.rules.scripts

import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts
import com.softwood.rules.core.BasicFact

import java.util.stream.Stream

//Facts has a hashMap as delegate so gets all map features as well
Facts facts = new Facts (name:"myfacts", description:"some facts")

println "facts instance called : " + facts.name

//facts.add (["sky": "isBlue"])
facts << ["sky": "isBlue"] << ["high":"noon"]
facts.putAll (["what": 0])

List list = facts.asList()
println list

Iterator iterable = facts.iterator()

List<Fact> listOfFacts = facts.asFact()
Fact fact1 = listOfFacts[0]

println "from proy we have $fact1, and name $fact1.name"


int count = facts.get$map().iterator().size()
Map.Entry first = facts.get$map().iterator().next()
Fact firstFact = new BasicFact(first)
println "firstFact name is $firstFact.name"

int numofEentries = facts.stream().count()
println "entries in stream counted as $numofEentries"

println "--> rintln stream of facts "
Stream<Fact> streamOfFacts = facts.stream()
streamOfFacts.forEach{ f -> println f.name}

// i.each {println "$it :  ${it.getClass()}" }
println "---> for node in iterable"
for (  node in iterable) {
  println node.key + "  : " + node.value
}



String f1 = facts.findFact("sky")
int num = facts.findFact ("what")
println "<--->"
println f1
println num
println facts.high


