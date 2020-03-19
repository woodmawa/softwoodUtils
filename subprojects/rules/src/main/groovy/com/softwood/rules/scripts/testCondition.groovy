package com.softwood.rules.scripts

import com.softwood.rules.api.Condition
import com.softwood.rules.api.RuleFactory
import com.softwood.rules.core.BasicCondition
import com.softwood.rules.core.ConditionClosure
import org.codehaus.groovy.runtime.ComposedClosure

/*
Closure myClos = {
    println "myClos this: $this, super: $this.class.superclass \n owners metaclass is: $owner.metaClass.class \nowner: $owner, super: $owner.class.superclass,  \ndelegate: $delegate"
    println "called with : $it"; true}
println "\t>>>dump: " + myClos.dump()
println "\t>>>with thisObject as: $myClos.thisObject, and class as: $myClos.thisObject.class"
myClos("hi william")
*/



Closure std = {println "std was called with '$it'"; true}
println "std dump is  " + std.dump()
Condition conClos = ConditionClosure.from (std )
//conClos << {println " added some : $it"; true}
println "conClos dump is " +  conClos.dump()
println ">>now invoke the closure "
boolean result = conClos.test ("hi william")

Closure addOne = {it  +1}
println "addOne instance is : " + addOne.toString()
Closure multiplyByThree = {it *3}
println "multiplyByThree instance is : " + multiplyByThree.toString()

Condition conClos2 = ConditionClosure.from {it}
println "conClos2 instance is : " + conClos2.toString()
def num = conClos2(2)
assert num == 2
println "conClos(2) == $num"

ComposedClosure composed1 = conClos2 >> addOne

num = composed1 (2)
assert num == 3
println "composed1 (2) == $num"

ComposedClosure composed2 = composed1 >> multiplyByThree
println "aggregated compose  of (2) is : " + composed2 (2)
num = composed2 (2)
println "composed2 (2) == $num"
assert num == 9

System.exit(1)

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

