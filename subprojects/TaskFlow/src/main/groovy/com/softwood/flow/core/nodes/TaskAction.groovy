package com.softwood.flow.core.nodes

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowEvent
import com.softwood.flow.core.flows.FlowType
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise

import java.util.concurrent.ConcurrentLinkedQueue

import static groovyx.gpars.dataflow.Dataflow.task

@Slf4j
class TaskAction extends AbstractFlowNode{
    def debug = false

    static TaskAction newAction (name = null,  Closure closure) {
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

        def ta = new TaskAction(ctx: ctx, name: name ?: "anonymous", action:closure)
        ta.ctx?.taskActions << ta
        if (ta.ctx?.flow)
            ta.ctx?.flow.defaultSubflow.flowNodes << ta


        if (ta.ctx.newInClosure != null)
            ta.ctx.newInClosure << ta  //add to items generated within the running closure
        ta

    }

    static TaskAction newAction (FlowContext ctx, name = null,  Closure closure) {
        /*
         * here we are injected with ctx to start
         */


        def ta = new TaskAction(ctx: ctx, name: name ?: "anonymous", action:closure)
        ta.ctx?.taskActions << ta
        if (ta.ctx?.flow)
            ta.ctx?.flow.defaultSubflow.flowNodes << ta


        if (ta.ctx.newInClosure != null)
            ta.ctx.newInClosure << ta  //add to items generated within the running closure
        ta

    }

    static TaskAction newAction (name = null,  long delay, Closure closure) {
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

        def ta = new TaskAction(ctx: ctx, taskDelay: delay, name: name ?: "anonymous", action:closure)
        ta.ctx?.taskActions << ta
        if (ta.ctx?.flow)
            ta.ctx?.flow.defaultSubflow.flowNodes << ta
        if (ta.ctx.newInClosure != null)
            ta.ctx.newInClosure << ta  //add to items generated within the running closure

        ta

    }


    /**
     * run the closure as task, by invoking private doRun().  result holds the answer as a DF promise
     * @param args - optional Object[]
     * @param errHandler - closure to call in case of exception being triggered
     * @return 'this' FlowNode
     */
    def run (args = null, Closure errHandler = null) {
        doRun (null, args, errHandler)
    }

    def run (AbstractFlowNode previousNode, args = null, Closure errHandler = null) {
        doRun (previousNode, args, errHandler)
    }
    def delayedRun (int delay, args = null, Closure errHandler = null) {
        taskDelay = delay
        run (args, errHandler)
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
    private def doRun  (AbstractFlowNode previousNode, Object[] args = null, Closure errHandler = null) {
        //println " doRun: made it to action task with previousTask $previousNode, and args as $args, and errHandler $errHandler"
        if (taskDelay == 0)
            status = FlowNodeStatus.running
        else
            status = FlowNodeStatus.deferred


        def ta = actionTask (previousNode, this, args, errHandler)
        ta
    }

    private def actionTask (TaskAction previousNode, AbstractFlowNode step,  Object[] args, Closure errHandler = null) {
        try {
            def cloned  = step.action.clone()
            cloned.delegate = step.ctx
            cloned.resolveStrategy = Closure.DELEGATE_FIRST


            //if deferred in time, sleep required period of ms
            if (taskDelay) {
                Thread.sleep( taskDelay, {println "thread interupted "})
                log.debug "ActionTask: finished delay $taskDelay ms, now scheduling task"
            }

            step.ctx?.flowListeners.each { listener ->
                listener.beforeFlowNodeExecuteState (step.ctx, this)
            }
            //schedule task and receive the future and store it
            //pass promise from this into new closure in the task

            Promise promise  = task {
                //println "in task() with step $step.name, has previousResult as $previousTaskResult and has closure with params with types $cloned.parameterTypes"
                def ans
                if (cloned.maximumNumberOfParameters == 2 &&
                        (cloned.parameterTypes[1] == Promise || cloned.parameterTypes[1] == DataflowVariable)) {
                    ans = cloned(step.ctx, previousNode.result)  //expecting result from this flowNode
                }
                else if (cloned.maximumNumberOfParameters == 2 && cloned.parameterTypes[1] == AbstractFlowNode)
                    ans = cloned(step.ctx, previousNode)
                else if (cloned.maximumNumberOfParameters == 2 && cloned.parameterTypes[1] == Object[])
                    ans = cloned(step.ctx, args)
                else if (cloned.maximumNumberOfParameters == 2 && cloned.parameterTypes[1] == Object)
                    ans = cloned(step.ctx, args)
                else if (cloned.parameterTypes?[0] == FlowContext && cloned.maximumNumberOfParameters > 2  )
                    ans = cloned(step.ctx, *args)
                else if (cloned.maximumNumberOfParameters == 1)
                    ans = cloned(step.ctx)
                else
                    ans = cloned()
                ans

            }
            step.result = promise
            step.ctx.activePromises << promise
            //when DF is bound remove promise from ctx.activePromises
            promise.whenBound {
                status = FlowNodeStatus.completed
                boolean yesNo = step.ctx?.activePromises.remove (promise)
                assert yesNo

                step.ctx?.flowListeners.each { listener ->
                    listener.afterFlowNodeExecuteState (ctx, this)
                    if(ctx.type = FlowType.Process) {
                        FlowEvent fe = new FlowEvent<>(flow: ctx.flow,  message: "completed task #$sequence with '$name' ", referencedObject: this)
                        listener.flowEventUpdate(ctx, fe)
                    }
                }

                log.debug "actionTask(): promise was bound with $it, removed promise $promise from activePromises: $yesNo, and activePromises : " + ctx?.activePromises

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

    def  then (AbstractFlowNode nextStep, args = null, Closure errHandler = null) {
        assert nextStep, "was expecting non null nextStep"

        //todo replace with log
        log.debug "then : fire <$nextStep.name> action "

        Object[] EMPTY_ARGS = []

        super.then (nextStep, errHandler)

        def previousTask = this
        actionTask (previousTask, nextStep, args ?: EMPTY_ARGS)

    }

    String toString () {
        if (debug == false)
            "Action (name:$name, status:$status)"
        else
            "Action (name:$name, status:$status, action:${action.toString()})"
    }
}

