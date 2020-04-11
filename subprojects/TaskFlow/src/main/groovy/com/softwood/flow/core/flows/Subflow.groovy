package com.softwood.flow.core.flows

import com.softwood.flow.core.nodes.AbstractFlowNode
import com.softwood.flow.core.nodes.FlowNodeStatus
import groovyx.gpars.dataflow.Promise

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

import static groovyx.gpars.dataflow.Dataflow.task

class Subflow extends AbstractFlow {

    protected AbstractFlow parent
    protected ConcurrentLinkedDeque<Promise> subFlowPromises = new ConcurrentLinkedDeque<>()
    protected ConcurrentLinkedDeque<AbstractFlowNode> flowNodes = new ConcurrentLinkedQueue<>()


    static Subflow newSubflow (flowName = null, Closure clos)  {

        Subflow sflow = new Subflow (name: flowName)
        sflow.flowType = FlowType.Subflow

        sflow

    }

    boolean hasTasks () {
        flowNodes.size () > 0
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
        firstStep.@result = promise

        firstStep
    }


}
