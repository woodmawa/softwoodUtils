package scripts

import com.softwood.flow.core.flows.Flow
import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.languageElements.Condition
import com.softwood.flow.core.nodes.ChoiceAction
import com.softwood.flow.core.nodes.TaskAction
import com.softwood.flow.core.support.CallingStackContext
import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.Field
import com.sun.jdi.IncompatibleThreadStateException
import com.sun.jdi.InvalidTypeException
import com.sun.jdi.InvocationException
import com.sun.jdi.Method
import com.sun.jdi.MonitorInfo
import com.sun.jdi.ObjectReference
import com.sun.jdi.ReferenceType
import com.sun.jdi.StackFrame
import com.sun.jdi.ThreadGroupReference
import com.sun.jdi.ThreadReference
import com.sun.jdi.Type
import com.sun.jdi.Value
import com.sun.jdi.VirtualMachine
import groovyx.gpars.dataflow.DataflowVariable

import java.util.concurrent.ConcurrentLinkedDeque


//this creates a context for the flow, and if closure presented as arg will set the delegate to the ctx
flow = Flow::newFlow

action = TaskAction::newAction
choice = ChoiceAction::newChoiceAction
flowCondition = Condition::newCondition

subflow = Subflow::newSubflow


Flow f = flow ('second flow') {
    //action (delegate, 'act#1') {delegate, args -> println "hello [$args]"; 1}
    //by putting DF variable in the closure you get the previous tasks result. doing the getVal() will sync on previous result
    //action (delegate, 'act#2') {delegate, DataflowVariable result ->  def ans = result.val; println "william [$ans]";2 }
    //action (delegate, 'act#3') {delegate, DataflowVariable result ->  def ans = result.val; println "today [$ans]";3 }

    println "subflow closure - create two actions "
    def act = action (delegate, 'main-act#1') {println "hello main act1 "; 1}
    action (delegate, 'main-act#2') {println "hello main act2 "; 2}
            .dependsOn (act)
    choice (delegate, 'my choice') {sel, args ->
        //use a dynamic condition as expressed in the condition closure
        when (sel, flowCondition {it == 'choiceSelect'} ) {
            action(delegate, 'subflow-act1') { println "sublow sf-act1"; 3.1}
        }
        //boolean
        when (true) {
            action(delegate, 'subflow-act2') { println "sublow sf-act2"; 3.2}
        }
        //closure<boolean> test
        when (sel, { it == 'choiceSelect' }) {
            action(delegate, 'subflow-act3') { println "sublow sf-act3"; 3.3}
        }
        'done choice'

    }.setSelectValue ('choiceSelect')  //directly set the sel//otherwise its the prevNode.resultValue

}

f.start('starting flow arg')

println "tasks run : " + f.ctx.taskActions
println "active promises reported as : " + f.ctx.activePromises
