package scripts

import com.softwood.flow.core.flows.Flow
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.nodes.TaskAction
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

f = flow ('second flow') {
    action ('act#1') { println "hello "; 1}
    action ('act#2') {ctx, DataflowVariable result ->  def ans = result.val; println "william [$ans]";2 }
    action ('act#3') {ctx, DataflowVariable result ->  def ans = result.val; println "today [$ans]";3 }
}

f

/*
flow << subflow ('another flow') {}
flow << action ('first action') {println 'hello william'}
*/

f.start()

println "tasks run : " + f.ctx.taskActions
println "active prmosises reported as : " + f.ctx.activePromises
//sleep (1000)
