package com.softwood.flow.core.nodes

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowEvent
import com.softwood.flow.core.flows.FlowStatus
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.support.CallingStackContext
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.Promise

import java.util.concurrent.ConcurrentLinkedQueue

import static groovyx.gpars.dataflow.Dataflow.task

@Slf4j
class ChoiceAction extends AbstractFlowNode {

    ConcurrentLinkedQueue<Subflow> choiceSubflows = new ConcurrentLinkedQueue<>()

    //static ChoiceAction newChoiceAction(FlowContext ctx, name = null, Closure closure) {
        /*
         *if we see an action declaration with closure, where the closure.owner is itself a closure, then check if the
         * closure delegate is an Expando - if so we assume that this Expando is the ctx of a parenting flow
         */
        /*List frames = CallingStackContext.getContext()
        boolean isCalledInClosure = frames ?[1].callingContextIsClosure */

        /*def ctx
        def owner = closure.owner
        def delegate = closure.delegate
        if (owner instanceof Closure &&
                delegate instanceof Closure &&
                delegate?.delegate instanceof FlowContext) {
            ctx = closure.delegate.delegate
        } else {
            if (isCalledInClosure) {
                //get context ??
            } else
                ctx = FlowContext.newFreeStandingContext()
        }*/

        /*
        def choice = new ChoiceAction(ctx: ctx, name: name ?: "anonymous", action: closure)
        choice.ctx?.taskActions << choice

        if (choice.ctx.newInClosure != null) {

            //add to list of newly created objects
            //ctx?.saveClosureNewIns(ctx.getLogicalAddress(sflow), sflow)
            //only add to newInClosure if its called within a closure
            if (isCalledInClosure)
                choice.ctx.newInClosure << choice  //add to items generated within the running closure
        }
        choice*/

    //}

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
            step.status = FlowNodeStatus.running

            //if we find padded nul then striup this off
            if (args?.size() > 1) {
                if (args[-1] == null) {
                    args = args.toList().subList(0, args.size() - 1)
                }
            }

            //schedule task and receive the future and store it
            //pass promise from this into new closure in the task
            Promise promise = task {
                def ans

                //todo  - where should the calculator live ?  here or in the original closure
                def selector = subflowSelector (previousNode?.result.val, args)
                ans = cloned(selector, *args)  //(calculated selector discrimator, args...)
            }
            step.result = promise
            step.ctx.activePromises << promise
            //when DF is bound remove promise from ctx.activePromises
            promise.whenBound {
                status = FlowNodeStatus.completed
                boolean yesNo = step.ctx?.activePromises.remove(promise)
                assert yesNo

                step.ctx?.flowListeners.each { listener ->
                    listener.afterFlowNodeExecuteState(ctx, this)
                    if (ctx.type = FlowType.Process) {
                        FlowEvent fe = new FlowEvent<>(flow: ctx.flow, message: "completed task #$sequence with '$name' ", referencedObject: this)
                        listener.flowEventUpdate(ctx, fe)
                    }
                }

                log.debug "choiceTask(): promise was bound with $it, removed promise $promise from activePromises: $yesNo, and activePromises : " + ctx?.activePromises

            }

            List newIns = ctx.newInClosure.toList()
            def newSubflows = newIns.grep {it.class == Subflow}
            if (ctx.flow) {
                newIns.each {it.parent = ctx.flow; ctx.flow.subflows << it}
            }
            choiceSubflows.addAll (newSubflows)
            ctx.newInClosure.clear()
            def preChoiceResult = previousNode.result.val
            //def selected = choiceSubflows.grep {subflowSelector(it, preChoiceResult)}
            //assert selected.size() == 1 //we are expecting a match

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

    //default selector logic for a choice
    def subflowSelector (def previousResult, args) {
        previousResult      //default 'null logic' position
    }

    String toString () {
        "Choice (name:$name, status:$status) with # of subflows ${choiceSubflows.size()}"
    }
}
