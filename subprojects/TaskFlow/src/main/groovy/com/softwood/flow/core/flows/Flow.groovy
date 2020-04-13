package com.softwood.flow.core.flows

import com.softwood.flow.core.nodes.AbstractFlowNode
import com.softwood.flow.core.nodes.FlowNodeStatus
import com.softwood.flow.core.nodes.TaskAction
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

import static groovyx.gpars.dataflow.Dataflow.task

import com.softwood.flow.core.flows.Subflow

class Flow extends AbstractFlow {
    protected ConcurrentLinkedDeque subflows = new ConcurrentLinkedDeque<>()
    protected defaultSubflow

    static Flow newFlow (String flowName = null, Closure closure = null)  {


        Flow flow = new Flow (name: flowName ?: "anonymous flow", flowType: FlowType.Process)
        Closure cloned = closure?.clone()
        cloned?.delegate = flow.ctx
        cloned?.resolveStrategy = Closure.DELEGATE_FIRST

        //captures any objects created in the closure
        flow.ctx.newInClosure = []

        if (cloned)
            cloned ()

        //process logic for any new instances added when closure was run
        flow.ctx.newInClosure.each {
        }

        flow
    }

    /*
     * constructor for flow
     */
    Flow () {
        def defaultFlow = new Subflow (name:'default subflow')
        defaultFlow.flowType = FlowType.DefaultSubflow
        defaultFlow.parent = this
        defaultSubflow = defaultFlow
        subflows << defaultFlow
        ctx = FlowContext.newProcessContext(this)  //create initialised context for whole flow and flow nodes
        this
    }

    def start (args = null) {
        start (0, args)
    }

    def start (int timeoutAfter, args) {
        Subflow sflow = defaultSubflow

        if (args) {
            //store initial flow starts args  on the context
            ctx.initialArgs = args
        }

        FlowEvent startEvent = new FlowEvent (flow: this, messsage: "starting flow $name")
        ctx.flowListeners.each {FlowListener listener ->
            listener.flowEventUpdate(this, startEvent)
        }

        if (sflow.hasTasks()) {
            //build list of promises from each task, present back in subsequent task runs()
            ConcurrentLinkedQueue<AbstractFlowNode> previousFlowNodes = new ConcurrentLinkedQueue ()
            sflow.flowNodes.toList().eachWithIndex{ AbstractFlowNode ta, int idx ->
                if (idx == 0) {
                    //if args pass to the first task, the rest will be maintained in the ctx
                    ta.run(args)
                    previousFlowNodes << ta
                }
                else {
                    def previousNode = previousFlowNodes[idx-1]
                    ta.run(previousNode)
                    previousFlowNodes << ta
                }
            }

        }

        //collect all the promises from actions that have been run and wait for all the promises to complete
        if (timeoutAfter > 0) {
            ctx.taskActions.collect{it.result}.toList()*.join(timeoutAfter, TimeUnit.MILLISECONDS)
            println "flow '$name' timedout"
        }
        else {
            ctx.taskActions.collect{it.result}.toList()*.join()
            println "flow '$name' completed"
        }

    }
    
    def leftShift (Subflow sflow) {
        subflows << sflow
        sflow.parent = this as AbstractFlow
        if (!sflow.ctx)
            sflow.ctx = this.ctx

        sflow
    }

    def leftShift (Subflow subflow = null, AbstractFlowNode step) {
        Subflow sflow = subflow ?: defaultSubflow

        sflow.flowNodes << step
        this
    }

    void registerFlowListener (FlowListener listener) {
        ctx.flowListeners.add (listener)
    }

    void registerFlowListener (FlowListener[] listeners) {
        ctx.flowListeners.addAll (listeners)
    }

    boolean removeFlowListener (FlowListener listener) {
        ctx.flowListeners.remove (listener)
    }
}
