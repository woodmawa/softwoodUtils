package scripts

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.languageElements.Condition
import com.softwood.flow.core.nodes.ChoiceAction
import com.softwood.flow.core.nodes.TaskAction
import org.codehaus.groovy.runtime.MethodClosure

MethodClosure action = TaskAction::newAction
MethodClosure choice = ChoiceAction::newChoiceAction
MethodClosure condition = Condition::newCondition
MethodClosure subflow = Subflow::newSubflow

FlowContext freeStandingCtx = FlowContext.newFreeStandingContext()

//choice (externalisedCtx, 'my choice', choiceClos)
Subflow defSubflow = subflow(freeStandingCtx, 'default subflow') {
    println "subflow closure - create two actions "
    action(delegate, 'main1') { println "hello act1 "; 1 }
    action(delegate, 'main2') { println "hello act2 "; 2 }
    choice(delegate, 'my choice') { sel, args ->
        when(true) {
            action(delegate, 'subflow-act1') { println "sublow sf-act1"; 3.1 }
        }
        when(true) {
            action(delegate, 'subflow-act2') { println "sublow sf-act2"; 3.2 }
        }

        'done choice'
    }
    return 0
}

//defSubflow << action ('main') {'opt1'} << choice
defSubflow.run('start')

