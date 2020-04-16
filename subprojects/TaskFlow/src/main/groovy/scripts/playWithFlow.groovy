package scripts

import com.softwood.flow.core.flows.Flow
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.nodes.TaskAction
import com.softwood.flow.core.support.CallingStackContext
import groovyx.gpars.dataflow.DataflowVariable

import java.util.concurrent.ConcurrentLinkedDeque


Expando context = new Expando ()
context.activePromises = new ConcurrentLinkedDeque<>()
context.taskActions = []

//this creates a context for the flow, and if closure presented as arg will set the delegate to the ctx
flow = Flow::newFlow

action = TaskAction::newAction

subflow = Subflow::newSubflow


context
//Flow flow = flow ('first flow')
//flow.ctx = context

t = action ('act#1') {println "hello "; 1}

Flow f = flow ('second flow') {
    List frames = CallingStackContext.getContext()
    println "frames from the flow closure called in newFlow()"
    frames.each { println it.description}
    action ('act#1') {ctx, args -> println "hello [$args]"; 1}
    //by putting DF variable in the closure you get the previous tasks result. doing the getVal() will sync on previous result
    action ('act#2') {ctx, DataflowVariable result ->  def ans = result.val; println "william [$ans]";2 }
    action ('act#3') {ctx, DataflowVariable result ->  def ans = result.val; println "today [$ans]";3 }
}

f


f.start('starting flow arg')

println "tasks run : " + f.ctx.taskActions
println "active prmosises reported as : " + f.ctx.activePromises
//sleep (1000)
