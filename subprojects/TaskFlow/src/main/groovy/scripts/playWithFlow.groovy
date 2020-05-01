package scripts

import com.softwood.flow.core.flows.Flow
import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.languageElements.Condition
import com.softwood.flow.core.nodes.ChoiceAction
import com.softwood.flow.core.nodes.CmdShellAction
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
subflow = Subflow::newSubflow


Flow f = flow ('second flow') {

    println "subflow closure - create action, cmd, (dependent) Action, and choice"

    def act = action ('main-act#1') {println "hello main act1 "; 1}
    cmdShell ('cmd with arg') {cmdWithArguments ('systeminfo')}
    action ('main-act#2') {println "hello main act2 "; 2}
            .dependsOn (act)
    choice ('my choice') {sel, args ->
        //use a dynamic condition as expressed in the condition closure
        when (sel, flowCondition {it == 'choiceSelect'} ) {
            action('subflow-act1') { println "sublow sf-act1"; 3.1}
        }
        //boolean
        when (true) {
            action('subflow-act2') { println "sublow sf-act2"; 3.2}
        }
        //closure<boolean> test
        when (sel, { it == 'choiceSelect' }) {
            action('subflow-act3') { println "sublow sf-act3"; 3.3}
        }
        'done choice'

    }.setSelectValue ('choiceSelect')  //directly set the sel//otherwise its the prevNode.resultValue

}

f.start('starting flow arg')

println "tasks run : " + f.ctx.taskActions
println "active promises reported as : " + f.ctx.activePromises
