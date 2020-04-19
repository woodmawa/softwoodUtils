package com.softwood.flow.core.nodes

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowEvent
import com.softwood.flow.core.flows.FlowStatus
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.support.CallingStackContext
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise

import java.util.concurrent.ConcurrentLinkedQueue

import static groovyx.gpars.dataflow.Dataflow.task

@Slf4j
class ChoiceAction extends AbstractFlowNode {

    ConcurrentLinkedQueue<Subflow> choiceSubflows = new ConcurrentLinkedQueue<>()

    static ChoiceAction newChoiceAction(FlowContext ctx, name = null, Closure closure) {
        /*
         * injected ctx to use
         */

        def choice = new ChoiceAction(ctx: ctx, name: name ?: "anonymous", action: closure)
        choice.ctx?.taskActions << choice

        if (choice.ctx.newInClosure != null) {
            List frames = CallingStackContext.getContext()
            boolean isCalledInClosure = frames ?[1].callingContextIsClosure

            //add to list of newly created objects
            //ctx?.saveClosureNewIns(ctx.getLogicalAddress(sflow), sflow)
            //only add to newInClosure if its called within a closure
            if (isCalledInClosure)
                choice.ctx.newInClosure << choice  //add to items generated within the running closure
        }
        choice

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

        def choice = choiceTask(previousNode, this, args, errHandler)
        choice
    }

    private def choiceTask(TaskAction previousNode, AbstractFlowNode step, args, Closure errHandler = null) {
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

            //as this is a choice node - no point running as a task
           //todo  - where should the calculator live ?  here or in the original closure
            def selector = subflowSelector (previousNode, args)
            //cloned delegate is the FlowContext so no point passing as a parameter
            if (args instanceof Object[])
                step.result << cloned(selector, *args)  //(calculated selector discriminator, args...)
            else
                step.result << cloned(selector, args)

            //when DF is bound remove promise from ctx.activePromises
            status = FlowNodeStatus.completed
            step.ctx?.flowListeners.each { listener ->
                listener.afterFlowNodeExecuteState(ctx, this)
                if (ctx.type = FlowType.Process) {
                    FlowEvent fe = new FlowEvent<>(flow: ctx.flow, message: "completed choice node (#$sequence) with '$name' ")
                    listener.flowEventUpdate(ctx, fe)
                }
            }

            List newIns = ctx.newInClosure.toList()
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
                log.debug "doRun()  hit exception $e"
                status = FlowNodeStatus.errors
                errHandler(e, this)
            }
            step
        }
    }

    //default selector logic for a choiceAction
    def subflowSelector (DataflowVariable previousNode, args) {

        def previousResult
        if (previousNode) {
            previousResult = previousNode.result.val
        }
        else
            previousResult  = args    //default 'null logic' position
    }

    String toString () {
        int sz = choiceSubflows.size()
        String insertTxt = "with # of subflows ${choiceSubflows.size()}"
        "Choice (name:$name, status:$status) ${sz ? insertTxt : ''}"
    }
}
