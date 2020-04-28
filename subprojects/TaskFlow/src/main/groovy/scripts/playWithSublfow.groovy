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
Subflow defSubflow= subflow (freeStandingCtx, 'default subflow') {
    println "subflow closure - create two actions "
    action (delegate, 'main1') {println "hello act1 "; 1}
    action (delegate, 'main2') {println "hello act2 "; 2}
    choice (freeStandingCtx, 'my choice') {sel, args ->
        when (true) {
            action(delegate, 'subflow') { println "sublow sf-act"; 3.1}
        }
        'done choice'
    }
    return 0
}

//defSubflow << action ('main') {'opt1'} << choice
defSubflow.run ('start')

