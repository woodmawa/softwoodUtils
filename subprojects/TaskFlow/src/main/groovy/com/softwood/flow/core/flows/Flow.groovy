package com.softwood.flow.core.flows

import com.softwood.flow.core.nodes.AbstractFlowNode
import com.softwood.flow.core.nodes.ChoiceAction

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class Flow extends AbstractFlow {
    protected ConcurrentLinkedDeque subflows = new ConcurrentLinkedDeque<>()
    protected defaultSubflow

    static Flow newFlow(String flowName = null, Closure closure = null) {


        Flow flow = new Flow(name: flowName ?: "anonymous flow", flowType: FlowType.Process)
        Closure cloned = closure?.clone()
        cloned?.delegate = flow.ctx
        cloned?.resolveStrategy = Closure.DELEGATE_FIRST

        if (cloned)
            cloned()

        flow.ctx?.newInClosure.each { action ->
            //process logic for any new instances added when closure was run
            if (action instanceof AbstractFlowNode)
                flow << action     //add the newIns actions to the defaultSubFlow
        }
        flow.ctx?.newInClosure.clear()

        flow
    }

    /*
     * constructor for flow
     */

    Flow() {
        ctx = FlowContext.newProcessContext(this)  //create initialised context for whole flow and flow nodes
        def defaultFlow = new Subflow(name: 'default subflow', ctx: ctx)
        defaultFlow.flowType = FlowType.DefaultSubflow
        defaultFlow.parent = this
        defaultSubflow = defaultFlow
        subflows << defaultFlow
        this
    }

    def start(args = null) {
        ctx.withNestedNewIns(this::doStart, 0, args)
    }

    def start(int timeoutAfter, args = null) {
        ctx.withNestedNewIns(this::doStart, timeoutAfter, args)
    }

    protected def doStart(int timeoutAfter, args) {
        Subflow sflow = defaultSubflow

        if (args) {
            //store initial flow starts args  on the context
            if (args.size() > 1)
                ctx.initialArgs.addAll(args)
            else if (arg)
                ctx.initialArgs.add(args)
        }

        FlowEvent startEvent = new FlowEvent(flow: this, messsage: "starting flow $name")
        ctx.flowListeners.each { FlowListener listener ->
            listener.flowEventUpdate(this, startEvent)
        }

        if (sflow.hasTasks()) {
            //build list of promises from each task, present back in subsequent task runs()
            ConcurrentLinkedQueue<AbstractFlowNode> previousFlowNodes = new ConcurrentLinkedQueue()
            sflow.subflowFlowNodes.toList().eachWithIndex { AbstractFlowNode ta, int idx ->
                if (idx == 0) {
                    //if args pass to the first task, the rest will be maintained in the ctx
                    ta.run(args)
                    previousFlowNodes << ta
                } else {
                    def previousNode = previousFlowNodes[idx - 1]
                    if (ta.class == ChoiceAction) {
                        (ChoiceAction) ta.fork(previousNode)
                    } else {
                        ta.run(previousNode)
                    }
                    previousFlowNodes << ta
                }
            }

        }

        //collect all the promises from actions that have been run and wait for all the promises to complete
        if (timeoutAfter > 0) {
            ctx.taskActions.collect { it.result }.toList()*.join(timeoutAfter, TimeUnit.MILLISECONDS)
            println "flow '$name' timedout"
        } else {
            ctx.taskActions.collect { it.result }.toList()*.join()
            println "flow '$name' completed"
        }

    }

    def leftShift(Subflow sflow) {
        subflows << sflow
        sflow.parent = this as AbstractFlow
        if (!sflow.ctx)
            sflow.ctx = this.ctx

        sflow
    }

    def leftShift(Subflow subflow = null, AbstractFlowNode step) {
        Subflow sflow = subflow ?: defaultSubflow

        sflow.subflowFlowNodes << step
        this
    }

    void registerFlowListener(FlowListener listener) {
        ctx.flowListeners.add(listener)
    }

    void registerFlowListener(FlowListener[] listeners) {
        ctx.flowListeners.addAll(listeners)
    }

    boolean removeFlowListener(FlowListener listener) {
        ctx.flowListeners.remove(listener)
    }
}
