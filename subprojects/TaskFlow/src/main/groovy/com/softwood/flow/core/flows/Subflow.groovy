package com.softwood.flow.core.flows

import com.softwood.flow.core.nodes.AbstractFlowNode
import com.softwood.flow.core.nodes.ChoiceAction
import com.softwood.flow.core.nodes.FlowNodeStatus
import com.softwood.flow.core.nodes.TaskAction
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
    def subflowInitialArgs = []

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

    def run (ArrayList arrayArg, args = null) {
        doRun (arrayArg, args)
    }

    protected def doRun(args) {

        Closure cloned = subflowClosure
        cloned.delegate = ctx
        cloned.resolveStrategy = Closure.DELEGATE_FIRST

        status = FlowStatus.running

        //save previous newIns and start with empty list for this closure
        ctx.newInClosureStack.push (ctx.newInClosure)
        ctx.newInClosureStack = new ConcurrentLinkedQueue<>()

        //run the attached closure
        cloned(args)

        subflowInitialArgs = args

        def newIns = ctx.newInClosure.grep {it instanceof AbstractFlowNode}
        if (newIns)  {
            subflowFlowNodes.addAll (ctx.newInClosure)
        }
        subflowFlowNodes.eachWithIndex { node, idx ->
            def promise
            switch (node?.getClass()) {
                case ChoiceAction :

                    promise = new DataflowVariable<>()
                    if (idx == 0){
                        promise << (ChoiceAction) node.fork (args)
                    } else {
                        def predessor = subflowFlowNodes[idx - 1]
                        node.previousNode = predessor
                        promise << (ChoiceAction) node.fork (predessor, args)
                    }
                    break

                case TaskAction :
                    if (idx == 0){
                        promise = node.run (args)
                    } else {
                        def predessor = subflowFlowNodes[idx - 1]
                        node.previousNode = predessor
                        promise = node.run (predessor, args)
                    }
                    break

                default :
                    break
            }
            promises << promise
            subFlowPromises << promise

        }

        ctx.newInClosure.clear()
        ctx.newInClosure = ctx.newInClosureStack.pop ()  //return state to saved position

        status = FlowStatus.completed

        this
    }

    protected def doRun(ArrayList arrayArg, args) {

        Closure cloned = subflowClosure
        cloned.delegate = ctx
        cloned.resolveStrategy = Closure.DELEGATE_FIRST

        status = FlowStatus.running

        cloned(args)

        //start with a fresh newInClosure list
        ctx.newInClosureStack.push (ctx.newInClosure)
        ctx.newInClosure = new ConcurrentLinkedQueue()

        def newIns = ctx.newInClosure.grep {it instanceof AbstractFlowNode}
        if (newIns)  {
            subflowFlowNodes.addAll (ctx.newInClosure)
        }
        subflowFlowNodes.eachWithIndex { node, idx ->
            def promise
            switch (node?.getClass()) {
                case ChoiceAction :
                    promise = new DataflowVariable<>()
                    if (idx == 0){
                        promise << (ChoiceAction) node.fork (args)
                    } else {
                        def predessor = subflowFlowNodes[idx - 1]
                        node.previousNode = predessor
                        promise << (ChoiceAction) node.fork (predessor, args)
                    }
                    break

                case TaskAction :
                    if (idx == 0){
                        promise = node.run (args)
                    } else {
                        def predessor = subflowFlowNodes[idx - 1]
                        node.previousNode = predessor
                        promise = node.run (predessor, args)
                    }
                    break

                default :
                    break
            }

            promises << promise
            subFlowPromises << promise

        }

        ctx.newInClosure.clear()
        ctx.newInClosure = ctx.newInClosureStack.pop()


        status = FlowStatus.completed
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
