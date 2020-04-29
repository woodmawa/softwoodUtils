package com.softwood.flow.core.nodes

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowEvent
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.support.CallingStackContext
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise

import static groovyx.gpars.dataflow.Dataflow.select  as gparsSelect

import java.util.concurrent.ConcurrentLinkedQueue

@Slf4j
class MergeAction extends AbstractFlowNode {

    ConcurrentLinkedQueue<Subflow> mergeSubflows = new ConcurrentLinkedQueue<>()

    Subflow defaultSubflow

    static MergeAction newMergeAction(FlowContext ctx, name = null, Closure closure) {
        /*
         * injected ctx to use
         */

        def merge = new MergeAction(ctx: ctx, name: name ?: "anonymous", action: closure)
        merge.ctx?.taskActions << merge

        if (merge.ctx.newInClosure != null) {
            List frames = CallingStackContext.getContext()
            boolean isCalledInClosure = frames ?[1].callingContextIsClosure

            //add to list of newly created objects
            //ctx?.saveClosureNewIns(ctx.getLogicalAddress(sflow), sflow)
            //only add to newInClosure if its called within a closure
            if (isCalledInClosure)
                merge.ctx.newInClosure << merge  //add to items generated within the running closure
        }
        merge

    }

    /**
     * run the closure as task, by invoking private doRun().  result holds the answer as a DF promise
     * @param args - optional Object[]
     * @param errHandler - closure to call in case of exception being triggered
     * @return 'this' FlowNode
     */
    def join(args = null, Closure errHandler = null) {
        doJoin(null, args, errHandler)
    }

    /**
     * internal routine to execute the closure as a gpars task, and places the promise into field 'result'
     * also sets onBound closure to remove the promise from the ctx.activePromises list.
     *
     * if passed an error handler will invoke that and set the flowNode status to 'errors' and returns the flow node
     *
     * @param errHandler
     * @return
     */
    private def doJoin(AbstractFlowNode previousNode, args = null, Closure errHandler = null) {

        def merge = mergeTask(previousNode, this, args, errHandler)
        merge
    }



    protected def mergeTask(TaskAction previousNode, AbstractFlowNode step, args, Closure errHandler = null) {
        try {
            Closure cloned = step.action.clone()
            cloned.delegate = step.ctx
            cloned.resolveStrategy = Closure.DELEGATE_FIRST

            step.ctx?.flowListeners.each { listener ->
                listener.beforeFlowNodeExecuteState(step.ctx, this)
            }

            //if we find padded nul then striup this off
            if (args instanceof Object[] && args?.size() > 1) {
                if (args[-1] == null) {
                    args = args.toList().subList(0, args.size() - 1)
                    //args = args.grep{it}
                }
            }

            step.status = FlowNodeStatus.running

            List toMergeSubflows = []
            def closureResult

            def closParamCount = cloned.maximumNumberOfParameters

            //cloned delegate is the FlowContext so no point passing as a parameter
            if (closParamCount == 1) {
                closureResult = cloned(toMergeSubflows)
            } else if (args instanceof Object[]) {
                closureResult = cloned(toMergeSubflows, *args)  //(calculated selector discriminator, args...)
            } else {
                closureResult = cloned(toMergeSubflows, args)
            }

            List<AbstractFlowNode> mergedSubflowLastActions
            if (toMergeSubflows.size() > 0 ) {
                mergedSubflowLastActions = toMergeSubflows.collect{it.subflowFlowNodes.asList().last() }
                if (mergedSubflowLastActions)
                    mergedSubflowLastActions.collect {it.result}*.join()
            }

            step.result << 'merged'

            //when DF is bound remove promise from ctx.activePromises
            status = FlowNodeStatus.completed
            step.ctx?.flowListeners.each { listener ->
                listener.afterFlowNodeExecuteState(ctx, this)
                if (ctx.type = FlowType.Process) {
                    FlowEvent fe = new FlowEvent<>(flow: ctx.flow, message: "completed merge node (#$sequence) with '$name' ")
                    listener.flowEventUpdate(ctx, fe)
                }
            }

            List newIns = ctx.newInClosure.toList()
            //should be one subflow out
            def newSubflows = newIns.grep {it.class == Subflow}
            def newActions = newIns.grep {it instanceof AbstractFlowNode}
            if (ctx.flow) {
                    newIns.each {it.parent = ctx.flow; ctx.flow.subflows << it}
            }
            ctx.newInClosure.clear()

            //subflow (ctx:ctx, name:"merge[${name}].defaultSublow" ) {}
            defaultSubflow = newSubflows.size() > 0 ? newSubflows[0] :  new Subflow(ctx: ctx, name: "merge[${name}].defaultSublow", subflowClosure:{})

            defaultSubflow.subflowFlowNodes.addAll (newActions)
            //as input provide the list of actions you have waited on
            if (args)
                defaultSubflow.run (mergedSubflowLastActions, args)
            else
                defaultSubflow.run (mergedSubflowLastActions)

           step
        } catch (Exception e) {
            if (errHandler) {
                log.debug "mergeTask()  hit exception $e"
                status = FlowNodeStatus.errors
                errHandler(e, this)
            }
            step
        }
    }

    /**
     * run the closure as task, by invoking private doRun().  result holds the answer as a DF promise
     * @param args - optional Object[]
     * @param errHandler - closure to call in case of exception being triggered
     * @return 'this' FlowNode
     */
    def select (args = null, Closure errHandler = null) {
        doSelect(null, args, errHandler)
    }

    def select (List<AbstractFlowNode> selectNodes, args = null, Closure errHandler = null) {
        doSelect (null, previousNode, args, errHandler)
    }

    /*def select (List<Subflow> selectfromSubflows, args = null, Closure errHandler = null) {

        assert selectfromSubflows

        List<AbstractFlowNode> nodes = selectfromSubflows.collect {sflow -> sflow.subflowFlowNodes?[-1]}

        doSelect (nodes, previousNode, args, errHandler)
    }*/

    protected doSelect (List<AbstractFlowNode> selectNodes, args = null, Closure errHandler = null) {
        ctx.withNestedNewIns(this::selectAction, selectNodes, previousNode, args, errHandler)

    }

    protected selectAction (List<AbstractFlowNode> selectNodes, args = null, Closure errHandler = null) {
        List<Promise> promises

        if (selectNodes) {
            promises = selectNodes.collect {it.result}

            Promise selectResult = gparsSelect (promises)

            def firstResult = selectResult().value
       }
    }

    //todo - need to think what this needs to look like default selector logic for a choiceAction
    List mergeSubflowSelector (DataflowVariable previousNode, args) {
        []
    }

    String toString () {
        int sz = mergeSubflows.size()
        String insertTxt = "with # of subflows ${mergeSubflows.size()}"
        "Merge (name:$name, status:$status) ${sz ? insertTxt : ''}"
    }
}
