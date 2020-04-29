package scripts

import com.softwood.flow.core.flows.Flow
import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.flows.Subflow
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

subflow = Subflow::newSubflow

//will generate and  use free standing context
t = action ('act#1') {println "hello "; 1}

Flow f = flow ('second flow') {
    action (delegate, 'act#1') {delegate, args -> println "hello [$args]"; 1}
    //by putting DF variable in the closure you get the previous tasks result. doing the getVal() will sync on previous result
    action (delegate, 'act#2') {delegate, DataflowVariable result ->  def ans = result.val; println "william [$ans]";2 }
    action (delegate, 'act#3') {delegate, DataflowVariable result ->  def ans = result.val; println "today [$ans]";3 }
}

f.start('starting flow arg')

println "tasks run : " + f.ctx.taskActions
println "active promises reported as : " + f.ctx.activePromises
//sleep (1000)

/*

    def c = delegate  //should be flow ctx
    assert c instanceof FlowContext

    List frames = CallingStackContext.getContext()
    println "frames from the flow closure called in newFlow()"
    frames.each { println it.description}


 */
