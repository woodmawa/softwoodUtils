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


//<!--------------------------->

// try building a condition
Closure condClosOut = {arg ->
    def ans = "william" == arg;
    println "in condition closure received $arg,  returning : $ans";
    ans}

Condition c1 = condition (condClosOut)

def res2 = c1.test('william')


// try building a choice
Closure choiceClos = {def selectorValue, choiceRunArgs ->
    println "choice closure got arg : $choiceRunArgs"

    def sf1, sf2
    //Condition cond = flowCondition (selectorValue) {it == 'opt1'}
    //if (cond.test()) {

    //this will be true so one subflow will be added to the newIns
    when (flowCondition (selectorValue) {it == 'opt1'}) {selVal ->
        println "inside when condition (opt1) inside choiceClosure selector: $selectorValue, args: $choiceRunArgs"
        sf1 = subflow(delegate, 'csf#1', 'opt1') {
            def a1 = action(delegate, 'sf1Act1#') { println "subflow 1, action 1, returnining 1.1"; 1.1 }
            action(delegate, 'sf1Act2#') { println "subflow 1, action 2, returnining 1.2"; 1.2 }.dependsOn (a1)
        }
    }

    //this will not be true so newIns entry will be generated here
    if (selectorValue == 'opt2') {
        println "inside when condition (opt2) inside choiceClosure selector: $selectorValue, args: $choiceRunArgs"

        sf2 = subflow(delegate, 'csf#2', 'opt2') {
            action(delegate, 'sf2Act1#') { println "subflow 2, action 1, returnining 1.1"; 1.1 }

        }
    }


}

freeStandingCtx = FlowContext.newFreeStandingContext()

ChoiceAction split = choice (freeStandingCtx, 'my choice', choiceClos)

split.fork('opt1')

split

freeStandingCtx.taskActions.collect {it.result}.join()


System.exit (0)

sleep 1000
choice

/* <!---------------------------> */
//Made this a closure so that the whens delegate can be set, and we can resolve 'newInClosure'
/*def when = {Condition someCondition, toDoArgs,  Closure toDo ->

    toDo.delegate = delegate
    toDo.resolveStrategy = Closure.DELEGATE_FIRST

    // assert delegate.is (externalisedCtx)  - worked

    //try and resolve newInClosure list on FlowContext delegate
    List nicl
    if (delegate?.newInClosure) {
        nicl = delegate.newInClosure
        nicl.add (someCondition)
    }

    if (someCondition && someCondition.test ()) {
        toDo (toDoArgs)
    } else
        false       //fail as default

}*/
