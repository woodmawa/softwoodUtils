package com.softwood.rules.scripts

import com.softwood.rules.api.Condition
import com.softwood.rules.api.RuleFactory
import com.softwood.rules.core.BasicCondition

import java.util.function.Predicate

def param = "william"
Closure tes1 = {5 > 10}
Closure tes2 = {20 > 10}

//not setting private attribute dynamicTest
Condition c1 = new BasicCondition(name: "c1#", description : "my first condition ", conditionTest: tes1)
c1.conditionTest = tes1  //this actually does the required set of the default

//factory now handles setting the dynamic test from map input
Condition c2 = RuleFactory.newCondition(name:'c2#', description: "my 2nd condition", dynamicTest: tes2)

def res1 = c1.test(param)
def res2 = c2.test(param)
println "res1 was $res1, and res2 was $res2"

boolean bool1 = c1
boolean bool2 = c1 as Boolean //have to force explicitly to get the cast

println "bool1 was $bool1, and bool2 was $bool2"
def c3 = c1 | c2

println "c3 is " + c3.class.name + " with dynamic test closure " + c3.dynamicTest

res = c3.test (param)
println "c3 as $res  "


assert res

