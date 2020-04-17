package scripts

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.languageElements.Condition

import com.softwood.flow.core.nodes.ChoiceAction
import com.softwood.flow.core.nodes.TaskAction
import org.codehaus.groovy.runtime.MethodClosure

//import static com.softwood.flow.core.languageElements.Condition.when

MethodClosure action = TaskAction::newAction
MethodClosure choice = ChoiceAction::newChoiceAction
MethodClosure condition = Condition::newCondition
MethodClosure subflow = Subflow::newSubflow

FlowContext freeStandingCtx

//Made this a closure so that the whens delegate can be set, and we can resolve 'newInClosure'
def when = {Condition conditionArg, toDoArgs,  Closure toDo ->

    toDo.delegate = delegate
    toDo.resolveStrategy = Closure.DELEGATE_FIRST

    // assert delegate.is (externalisedCtx)  - worked

    //try and resolve newInClosure list on FlowContext delegate
    List nicl
    if (delegate?.newInClosure) {
        nicl = delegate.newInClosure
        nicl.add (conditionArg)
    }

    if (conditionArg && conditionArg.test ()) {
        toDo (toDoArgs)
    } else
        false       //fail as default

}
//<!--------------------------->

// try building a condition
Closure condClosOut = {arg ->
    def ans = "william" == arg;
    println "in condition closure received $arg,  returning : $ans";
    ans}

Condition c1 = condition (condClosOut)

def res2 = c1.test('william')


// try building a choice
Closure choiceClos = {ctx, choiceRunArgs ->
    println "choice closure got arg : $choiceRunArgs"

    subflow (delegate, 'csf#1', 'opt1') {
        action(delegate, 'sf1Act1#') { println "subflow 1, action 1, returnining 1.1"; 1.1 }
    }

    subflow (delegate, 'csf#2', 'opt2') {
        action(delegate, 'sf2Act1#') { println "subflow 2, action 1, returnining 1.2"; 1.2 }

    }

    //need this to set the FlowContext delegate on the 'when' closure.
    //todo - in flow class we'll neeed to set this explicitly in there before we use
    when.delegate = delegate
    when (condition (delegate, choiceRunArgs) {
        def ans = "william" == it;
        println "in condition closure recived $it,  returning : $ans";
        ans}, "with these ToDo args") {
            println "when: called with it = '$it' >> hello there $choiceRunArgs"
        }
}

freeStandingCtx = FlowContext.newFreeStandingContext()

ChoiceAction split = choice (freeStandingCtx, 'my choice', choiceClos)
split.run('william')

split

System.exit (0)

freeStandingCtx = FlowContext.newFreeStandingContext()
//choice (externalisedCtx, 'my choice', choiceClos)
Subflow defSubflow= subflow (freeStandingCtx, 'default subflow') {
    println "subflow closure - create two actions "
    action (delegate, 'main1') {println "hello act1 "; 1}
    action (delegate, 'main2') {println "hello act2 "; 2}
    choice (freeStandingCtx,'my choice', choiceClos)
    return 0
    //action (externalisedCtx, 'main2') {'opt1'}
}

//defSubflow << action ('main') {'opt1'} << choice
defSubflow.run ('start')


sleep 1000
choice