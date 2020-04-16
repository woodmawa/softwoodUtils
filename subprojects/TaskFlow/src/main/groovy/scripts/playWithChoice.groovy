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

def externalisedCtx

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
Closure choiceClos = {choiceRunArgs ->
    println "choice closure got arg : $choiceRunArgs"

    def ctx = delegate
    subflow (ctx, 'csf#1', 'opt1') {
        action('sf1Act1#') { println "subflow 1, action 1, returnining 1.1"; 1.1 }
    }
    subflow (ctx, 'csf#2', 'opt2') {
        action('sf2Act1#') { println "subflow 2, action 1, returnining 1.2"; 1.2 }

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

//ChoiceAction split = choice ('my choice', choiceClos)

//make ctx visible before running
//externalisedCtx = choice.ctx

//choice.run('william')

externalisedCtx = FlowContext.newFreeStandingContext()

//choice (externalisedCtx, 'my choice', choiceClos)
Subflow defSubflow= subflow (externalisedCtx, 'default subflow') {
    println "subflow closure - create two actions "
    action (externalisedCtx, 'main1') {println "hello act1 "; 1}
    action (externalisedCtx, 'main2') {println "hello act2 "; 2}
    choice (externalisedCtx,'my choice', choiceClos)
    return 0
    //action (externalisedCtx, 'main2') {'opt1'}
}

//defSubflow << action ('main') {'opt1'} << choice
defSubflow.run ('start')


sleep 1000
choice