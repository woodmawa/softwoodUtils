package com.softwood.flow.core.flows

import com.softwood.flow.core.nodes.AbstractFlowNode
import com.softwood.flow.core.nodes.FlowNodeStatus
import com.softwood.flow.core.support.CallingStackContext
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

import static groovyx.gpars.dataflow.Dataflow.task

class Subflow extends AbstractFlow {

    protected AbstractFlow parent
    protected ConcurrentLinkedDeque<Promise> subFlowPromises = new ConcurrentLinkedDeque<>()
    protected ConcurrentLinkedDeque<AbstractFlowNode> subflowFlowNodes = new ConcurrentLinkedQueue<>()
    def selectTag //todo make an optional??
    Closure subflowClosure

    static Subflow newSubflow (FlowContext ctx, String subFlowName = null, Closure clos)  {

        Subflow sflow = new Subflow (name: subFlowName, ctx:ctx, subflowClosure: clos)
        sflow.flowType = FlowType.Subflow

        List frames = CallingStackContext.getContext()
        boolean isCalledInClosure = frames?[1].callingContextIsClosure

        //add to list of newly created objects
        //ctx?.saveClosureNewIns(ctx.getLogicalAddress(sflow), sflow)
        //only add to newInClosure if its called within a closure
        if (isCalledInClosure)
            ctx.newInClosure << sflow

        sflow

    }

    static Subflow newSubflow (FlowContext ctx, String subFlowName = null, selectTag,   Closure clos)  {

        Subflow sflow = new Subflow (name: subFlowName, ctx:ctx, selectTag: selectTag, subflowClosure: clos)
        sflow.flowType = FlowType.Subflow

        List frames = CallingStackContext.getContext()
        boolean isCalledInClosure = frames?[1].callingContextIsClosure

        //add to list of newly created objects
        //ctx?.saveClosureNewIns(ctx.getLogicalAddress(sflow), sflow)
        //only add to newInClosure if its called within a closure
        if (isCalledInClosure)
            ctx.newInClosure << sflow

        sflow

    }

    boolean hasTasks () {
        subflowFlowNodes.size () > 0
    }

    def run (args = null) {
        doRun (args)
    }

    private def doRun(args) {

        Closure cloned = subflowClosure
        cloned.delegate = ctx
        cloned.resolveStrategy = Closure.DELEGATE_FIRST

        cloned(args)

        def newIns = ctx.newInClosure.grep {it instanceof AbstractFlowNode}
        if (newIns)  {
            subflowFlowNodes.addAll (ctx.newInClosure)
        }
        subflowFlowNodes.eachWithIndex { node, idx ->
            def promise
            if (idx == 0){
                promise = node.run (args)
            } else
                promise = node.run ()
            promises << promise
            subFlowPromises << promise

        }

        ctx.newInClosure.clear()
        this
    }

    def leftShift (AbstractFlowNode firstStep) {
        //todo
    }

    def rightShift (AbstractFlowNode firstStep ) {

        firstStep.name =  "subflow initial step"
        println "subflow rightShift on first node $firstStep.name "

        Closure cloned  = firstStep.action.clone()

        cloned.delegate = ctx
        firstStep.ctx = ctx
        firstStep.status = FlowNodeStatus.running

        Promise promise =  task {def ans = cloned(ctx)
            ans
        }
        promise.whenBound {
            def yesNo = ctx.activePromises.remove (promise)
            assert yesNo
            firstStep.status = FlowNodeStatus.completed

        }
        ctx.activePromises << promise
        promises << promise
        subFlowPromises << promise
        firstStep.result = promise

        firstStep
    }


}
