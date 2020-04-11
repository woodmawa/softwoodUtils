package com.softwood.flow.core.nodes

import groovyx.gpars.dataflow.DataflowVariable

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import com.softwood.flow.core.flows.FlowContext

enum FlowNodeStatus {
    ready, running, deferred, completed, errors
}

abstract class AbstractFlowNode {
    static AtomicLong sequenceGenerator = new AtomicLong (0)

    protected Closure action

    protected FlowContext ctx    //has to be static to get splits 'action's' to have a ctx
    protected AbstractFlowNode previousNode
    protected Closure cloned  //'then' clones the closure on the next Action and invokes that
    protected final sequence = sequenceGenerator.incrementAndGet()
    //protected async = false

    String name = "anonymous"
    FlowNodeStatus status = FlowNodeStatus.ready
    protected DataflowVariable result = new DataflowVariable()
    long taskDelay = 0

    void setResultValue (value) {
        result << value
    }

    //provide a version that unwraps the DF result to get the value
    def  getResultValue () {
        result.getVal()     //blocking get on DF result
    }

    def  getResultValue (long timeout, TimeUnit unit) {
        result.getVal(timeout, unit)     //blocking get on DF result
    }

    def then (AbstractFlowNode nextStep, Closure errhandler = null) {
        cloned  = nextStep.action.clone()
        if (nextStep != this)
            nextStep.previousNode = this
        if (taskDelay == 0)
            status = FlowNodeStatus.running
        else
            status = FlowNodeStatus.deferred

        cloned.delegate = ctx

        if (!nextStep.ctx)
            nextStep.ctx = this.ctx

        //check if any promises have completed and if so remove from list

        nextStep

    }

    //syntactic sugar
    def rightShift (AbstractFlowNode nextStep,  Closure errhandler = null) {
        then (nextStep, errhandler)
    }



}


