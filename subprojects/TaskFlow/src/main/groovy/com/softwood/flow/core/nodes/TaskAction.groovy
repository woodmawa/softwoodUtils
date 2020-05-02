package com.softwood.flow.core.nodes

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowEvent
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.support.CallingStackContext
import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise

import java.util.concurrent.ConcurrentLinkedQueue

import static groovyx.gpars.dataflow.Dataflow.task

@Slf4j
class TaskAction extends AbstractFlowNode{
    def debug = false

    static TaskAction newAction (String name = null, Map initArgsMap = null, Closure closure) {
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

        def ta
        if (!initArgsMap)
            ta = new TaskAction(ctx: ctx, name: name ?: "anonymous", action:closure)
        else
            ta = new TaskAction(ctx: ctx, name: name ?: "anonymous", action:closure, *:initArgsMap)

        ta.ctx?.taskActions << ta
        ta.ctx?.newInClosure << ta

        /*if (ta.ctx.newInClosure != null) {
            List frames = CallingStackContext.getContext()
            boolean isCalledInClosure = frames ?[1..3].any {it.callingContextIsClosure}

            //add to list of newly created objects
            //ctx?.saveClosureNewIns(ctx.getLogicalAddress(sflow), sflow)
            //only add to newInClosure if its called within a closure
            if (isCalledInClosure)
                ta.ctx.newInClosure << ta  //add to items generated within the running closure
        }*/

        ta

    }

    static TaskAction newAction (FlowContext ctx, String name = null, Map initArgsMap = null, Closure closure) {
        /*
         * here we are injected with ctx to start
         */

        assert ctx
        def ta
        if (!initArgsMap)
            ta = new TaskAction(ctx: ctx, name: name ?: "anonymous", action:closure)
        else
            ta = new TaskAction(ctx: ctx, name: name ?: "anonymous", action:closure, *:initArgsMap)

        ta.ctx?.taskActions << ta
        ta.ctx?.newInClosure << ta

        ta

    }

    static TaskAction newAction (FlowContext ctx, name = null,  Map initArgsMap = null, long delay, Closure closure) {

        def ta
        if (!initArgsMap)
            ta = new TaskAction(ctx: ctx, taskDelay: delay, name: name ?: "anonymous", action:closure)
        else
            ta = new TaskAction(ctx: ctx, taskDelay: delay, name: name ?: "anonymous", action:closure, *:initArgsMap)

        ta.ctx?.taskActions << ta
        if (ta.ctx?.flow)
            ta.ctx?.flow.defaultSubflow.flowNodes << ta
        ta.ctx?.newInClosure << ta  //add to items generated within the running closure

        ta

    }


    /**
     * run the closure as task, by invoking private doRun().  result holds the answer as a DF promise
     * @param args - optional Object[]
     * @param errHandler - closure to call in case of exception being triggered
     * @return 'this' FlowNode
     */
    def run (ArrayList arrayArg, args = null, Closure errHandler = null) {
        doRun (null, arrayArg, args, errHandler)
    }

    def run (args = null, Closure errHandler = null) {
        doRun (null, null, args, errHandler)
    }

    def run (AbstractFlowNode previousNode, args = null, Closure errHandler = null) {
        doRun (previousNode, null, args, errHandler)
    }

    def delayedRun (int delay, args = null, Closure errHandler = null) {
        taskDelay = delay
        run (null, null, args, errHandler)
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
    protected def doRun  (AbstractFlowNode previousNode, ArrayList arrayArg, args = null, Closure errHandler = null) {
        //println " doRun: made it to action task with previousTask $previousNode, and args as $args, and errHandler $errHandler"
        if (taskDelay == 0)
            status = FlowNodeStatus.running
        else
            status = FlowNodeStatus.deferred


        def ta = actionTask (previousNode, this, arrayArg, args, errHandler)
        ta
    }

    protected def actionTask (AbstractFlowNode previousNode, AbstractFlowNode step, ArrayList arrayArg, args, Closure errHandler = null) {
        try {
            Closure cloned  = step.action.clone()
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

            //if this node is declared with previous dependent tasks
            waitForDependencies(step)


            Promise promise  = task {
                //println "in task() with step $step.name, has previousResult as $previousTaskResult and has closure with params with types $cloned.parameterTypes"
                def ans
                if (cloned.maximumNumberOfParameters == 2 && cloned.parameterTypes?[0] == FlowContext  ){
                    ans = cloned(step.ctx, args)
                } else if (cloned.maximumNumberOfParameters > 2 && cloned.parameterTypes?[0] == FlowContext  ){
                    ans = cloned(step.ctx, *args)
                } else if (cloned.maximumNumberOfParameters == 2  && (cloned.parameterTypes?[0] == Object || cloned.parameterTypes?[0] == FlowContext) && (cloned.parameterTypes[1] == Promise || cloned.parameterTypes[1] == DataflowVariable) ){
                    ans = cloned(step.ctx, previousNode.result)
                } else if (cloned.maximumNumberOfParameters == 2 && cloned.parameterTypes[0] == ArrayList && args.size() > 0)
                    ans = cloned(arrayArg, *args)
                else if (cloned.parameterTypes[0] == Promise || cloned.parameterTypes[0] == DataflowVariable) {
                    ans = cloned(previousNode.result)  //expecting result from this flowNode
                }
                else if  (cloned.maximumNumberOfParameters == 1 && cloned.parameterTypes?[0] == FlowContext || cloned.parameterTypes?[0] == Expando)
                    ans = cloned (step.ctx)
                else if  (cloned.maximumNumberOfParameters == 1 && cloned.parameterTypes?[0] == Object && args == null)
                    ans = cloned (step.ctx)
                else if (cloned.maximumNumberOfParameters == 1 && cloned.parameterTypes[0] == Object && args != null)
                    ans = cloned(args)
                else if (cloned.maximumNumberOfParameters == 1 && cloned.parameterTypes[0] instanceof AbstractFlowNode)
                    ans = cloned(previousNode)
                else if (cloned.maximumNumberOfParameters == 1 && cloned.parameterTypes[0] == ArrayList)
                    ans = cloned(arrayArg)
                else if (cloned.maximumNumberOfParameters == 1 && cloned.parameterTypes[0] == Object[] && args.size() > 0)
                    ans = cloned(*args)
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

                if (resultValue instanceof Exception) {
                    if (errHandler) {
                        log.debug "doRun(), task hit exception $resultValue"
                        status = FlowNodeStatus.errors
                        this.errors << resultValue
                        errHandler(resultValue, this)
                    }

                }
                log.debug "actionTask(): promise was bound with $it, removed promise $promise from activePromises: $yesNo, and activePromises : " + ctx?.activePromises

            }
            step

        } catch (Exception e) {
            if (errHandler) {
                log.debug "doRun()  hit exception $e"
                status = FlowNodeStatus.errors
                this.errors << e
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
         actionTask (previousTask, nextStep, null, args ?: EMPTY_ARGS)

    }

    String toString () {
        if (debug == false)
            "Action (name:$name, status:$status)"
        else
            "Action (name:$name, status:$status, action:${action.toString()})"
    }
}

