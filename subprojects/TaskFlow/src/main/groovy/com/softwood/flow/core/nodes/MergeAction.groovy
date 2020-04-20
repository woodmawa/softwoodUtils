package com.softwood.flow.core.nodes

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowEvent
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.support.CallingStackContext
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowVariable

import java.util.concurrent.ConcurrentLinkedQueue

@Slf4j
class MergeAction extends AbstractFlowNode {

    ConcurrentLinkedQueue<Subflow> mergeSubflows = new ConcurrentLinkedQueue<>()

    static MergeAction newChoiceAction(FlowContext ctx, name = null, Closure closure) {
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
    def run(args = null, Closure errHandler = null) {
        doRun(null, args, errHandler)
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
    private def doRun(AbstractFlowNode previousNode, args = null, Closure errHandler = null) {

        def merge = mergeTask(previousNode, this, args, errHandler)
        merge
    }

    private def mergeTask(TaskAction previousNode, AbstractFlowNode step, args, Closure errHandler = null) {
        try {
            def cloned = step.action.clone()
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

            List toMergeSubflows = {[]}
            //cloned delegate is the FlowContext so no point passing as a parameter
            if (args instanceof Object[])
                step.result << cloned(toMergeSubflows, *args)  //(calculated selector discriminator, args...)
            else
                step.result << cloned(toMergeSubflows, args)

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
            if (ctx.flow) {
                    newIns.each {it.parent = ctx.flow; ctx.flow.subflows << it}
            }
            choiceSubflows.addAll (newSubflows)
            ctx.newInClosure.clear()
            //for each subflow declared in the choice closure execute each subflow and its nodes
            choiceSubflows.each {sflow ->
                //for each flow that makes it run each flow
                sflow.run (ctx.flowNodeResults)
            }

           step
        } catch (Exception e) {
            if (errHandler) {
                log.debug "choiceTask()  hit exception $e"
                status = FlowNodeStatus.errors
                errHandler(e, this)
            }
            step
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
