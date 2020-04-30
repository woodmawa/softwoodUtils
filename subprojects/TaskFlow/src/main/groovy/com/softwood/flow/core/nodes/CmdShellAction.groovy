package com.softwood.flow.core.nodes

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowEvent
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.languageElements.CommandWithArgumentList
import com.softwood.flow.core.support.CallingStackContext
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.Promise

import static groovyx.gpars.dataflow.Dataflow.task

@Slf4j
class CmdShellAction extends AbstractFlowNode {
    def debug = false

    static CmdShellAction newCmdShellAction (name = null, Closure closure) {
        FlowContext ctx = FlowContext.newFreeStandingContext()

        newCmdShellAction (ctx, name, closure)


    }

    static CmdShellAction newCmdShellAction (FlowContext ctx, name = null, Closure closure) {

        def ca = new CmdShellAction(ctx: ctx, name: name ?: "anonymous", action:closure)
        ca.ctx?.taskActions << ca


        if (ca.ctx.newInClosure != null) {
            List frames = CallingStackContext.getContext()
            boolean isCalledInClosure = frames ?[1].callingContextIsClosure

            //add to list of newly created objects
            //ctx?.saveClosureNewIns(ctx.getLogicalAddress(sflow), sflow)
            //only add to newInClosure if its called within a closure
            if (isCalledInClosure)
                ca.ctx.newInClosure << ca  //add to items generated within the running closure
        }
        ca
    }

    static CmdShellAction newCmdShellAction (FlowContext ctx, name = null,  long delay, Closure closure) {

        def ca = new CmdShellAction(ctx: ctx, taskDelay: delay, name: name ?: "anonymous", action:closure)
        ca.ctx?.taskActions << ca

        if (ca.ctx.newInClosure != null) {
            List frames = CallingStackContext.getContext()
            boolean isCalledInClosure = frames ?[1].callingContextIsClosure

            //add to list of newly created objects
            //ctx?.saveClosureNewIns(ctx.getLogicalAddress(sflow), sflow)
            //only add to newInClosure if its called within a closure
            if (isCalledInClosure)
                ca.ctx.newInClosure << ca  //add to items generated within the running closure
        }

        ca

    }


    /**
     * run the closure as task, by invoking private doRun().  result holds the answer as a DF promise
     * @param args - optional Object[]
     * @param errHandler - closure to call in case of exception being triggered
     * @return 'this' FlowNode
     */
    def run (Object[] args, Closure errHandler = null) {
        doRun (null, args, errHandler)
    }

    def run (Closure errHandler = null) {
        doRun (null, null, errHandler)
    }

    def run (AbstractFlowNode previousNode, Object[] args = null, Closure errHandler = null) {
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
    private def doRun  (AbstractFlowNode previousNode, args = null, Closure errHandler = null) {
        //println " doRun: made it to action task with previousTask $previousNode, and args as $args, and errHandler $errHandler"
        if (taskDelay == 0)
            status = FlowNodeStatus.running
        else
            status = FlowNodeStatus.deferred


        def ta = ctx.withNestedNewIns(this::cmdActionTask, previousNode, this, args, errHandler)
        ta
    }

    protected def cmdActionTask (AbstractFlowNode previousNode, CmdShellAction step,  Object[] args, Closure errHandler = null) {
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

            cloned()
            List cmdArgs = []
            String command = ""
            if (ctx.newInClosure) {
                List<CommandWithArgumentList> cal = ctx.newInClosure.grep {it instanceof CommandWithArgumentList}.asList()

                cal.each {
                    command = it.name               //keeps overwritting the name
                    cmdArgs.addAll(it.toList())
                }
            }

            //args can get padded with null arg at the end of the list.  So if see the null, then strip it off
            if (args != null ) {
                def size = args.size()
                if (args[size - 1] == null)
                    args = args.toList().subList(0, size - 1)
            }

            def processArgs = []
            processArgs.addAll(cmdArgs)
            processArgs.addAll (args ?: [])

            Promise promise  = task {
                ProcessBuilder processBldr = new ProcessBuilder ("cmd", "/c", command, *processArgs)
                def process = processBldr.start()
                def ans = process.text
                int exitCode = process.waitFor();
                //todo throw exception if we see error code ?

                exitCode == 0 ? ans : exitCode

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

                log.debug "CmdShell actionTask(): promise was bound, removed the promise from activePromises: $yesNo, and activePromises : " + ctx?.activePromises

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
            "CmdShellAction (name:$name, status:$status)"
        else
            "CmdShellAction (name:$name, status:$status, action:${action.toString()})"
    }
}

