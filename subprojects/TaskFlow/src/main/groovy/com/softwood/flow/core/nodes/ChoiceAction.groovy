package com.softwood.flow.core.nodes

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowEvent
import com.softwood.flow.core.flows.FlowStatus
import com.softwood.flow.core.flows.FlowType
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.Promise

import static groovyx.gpars.dataflow.Dataflow.task

@Slf4j
class ChoiceAction extends AbstractFlowNode {

    static ChoiceAction newChoiceAction(name = null, Closure closure) {
        /*
         *if we see an action declaration with closure, where the closure.owner is itself a closure, then check if the
         * closure delegate is an Expando - if so we assume that this Expando is the ctx of a parenting flow
         */
        def ctx
        def owner = closure.owner
        def delegate = closure.delegate
        if (owner instanceof Closure &&
                delegate instanceof Closure &&
                delegate?.delegate instanceof FlowContext) {
            ctx = closure.delegate.delegate
        } else {
            ctx = FlowContext.newFreeStandingContext()
        }

        def choice = new ChoiceAction(ctx: ctx, name: name ?: "anonymous", action: closure)
        choice.ctx?.taskActions << choice

        if (choice.ctx.newInClosure != null)
            choice.ctx.newInClosure << choice  //add to items generated within the running closure
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
    private def doRun(AbstractFlowNode previousNode, Object[] args = null, Closure errHandler = null) {

        def choice = choiceTask(previousNode, this, args, errHandler)
        choice
    }

    private def choiceTask(TaskAction previousNode, AbstractFlowNode step, Object[] args, Closure errHandler = null) {
        try {
            def cloned = step.action.clone()
            cloned.delegate = step.ctx
            cloned.resolveStrategy = Closure.DELEGATE_FIRST

            step.ctx?.flowListeners.each { listener ->
                listener.beforeFlowNodeExecuteState(step.ctx, this)
            }
            step.status = FlowNodeStatus.running

            //if we find padded nul then striup this off
            if (args) {
                if (args[-1] == null) {
                    args = args.toList().subList(0, args.size() - 1)
                }
            }

            //schedule task and receive the future and store it
            //pass promise from this into new closure in the task
            Promise promise = task {
                def ans

                ans = cloned(*args)
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
}
