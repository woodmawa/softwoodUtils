package scripts

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.languageElements.Condition
import org.codehaus.groovy.runtime.MethodClosure

MethodClosure flowCondition = Condition::newCondition

FlowContext ctx = FlowContext.newFreeStandingContext()

//this version pre saves the arg
Condition cond = flowCondition('opt1') { it == 'opt1' }


//most natural form of expression ?
ctx.when(flowCondition('opt3') { it == 'opt3' }) {
    println "passed again with def cond arg  <$it>"
}

//alternate form pass the arg into test to trigger the predicate eval ?
ctx.when(flowCondition { it == 'opt3' }.test('opt3'), 'a do closure arg ') {
    println "test() called, passed again <$it>"
}

ctx.when({ true }, 'a do closure arg ') { println "passed again <$it>" }

if (cond.test())
    println "passed"


if (cond.test('opt2'))
    println "passed"
else
    println 'failed'

cond = flowCondition { it == 'opt3' }

if (cond.test('opt3'))
    println "passed"

