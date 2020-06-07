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
class PowerShellAction extends AbstractFlowNode {
    def debug = false

    static PowerShellAction newPowerShellAction(String name = null, Closure closure) {
        FlowContext ctx = FlowContext.newFreeStandingContext()

        newPowerShellAction(ctx, name, closure)


    }

    static PowerShellAction newPowerShellAction(FlowContext ctx, String name = null, Closure closure) {

        def psh = new PowerShellAction(ctx: ctx, name: name ?: "anonymous", action: closure)
        psh.ctx?.taskActions << psh
        psh.ctx?.newInClosure << psh  //add to items generated within the running closure

        psh
    }

    static PowerShellAction newPowerShellAction(FlowContext ctx, name = null, long delay, Closure closure) {

        def psh = new PowerShellAction(ctx: ctx, taskDelay: delay, name: name ?: "anonymous", action: closure)
        psh.ctx?.taskActions << psh
        psh.ctx?.newInClosure << psh  //add to items generated within the running closure

        psh
    }


    /**
     * run the closure as task, by invoking private doRun().  result holds the answer as a DF promise
     * @param args - optional Object[]
     * @param errHandler - closure to call in case of exception being triggered
     * @return 'this' FlowNode
     */
    def run(Object[] args, Closure errHandler = null) {
        doRun(null, args, errHandler)
    }

    def run(Closure errHandler = null) {
        doRun(null, null, errHandler)
    }

    def run(AbstractFlowNode previousNode, Object[] args = null, Closure errHandler = null) {
        doRun(previousNode, args, errHandler)
    }

    def delayedRun(int delay, args = null, Closure errHandler = null) {
        taskDelay = delay
        run(args, errHandler)
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
        //println " doRun: made it to action task with previousTask $previousNode, and args as $args, and errHandler $errHandler"
        if (taskDelay == 0)
            status = FlowNodeStatus.running
        else
            status = FlowNodeStatus.deferred


        def cmd = ctx.withNestedNewIns(this::pshActionTask, previousNode, this, args, errHandler)
        cmd
    }

    protected def pshActionTask(PowerShellAction previousNode, AbstractFlowNode step, Object[] args, Closure errHandler = null) {
        try {

            def cloned = step.action.clone()
            cloned.delegate = step.ctx
            cloned.resolveStrategy = Closure.DELEGATE_FIRST


            //if deferred in time, sleep required period of ms
            if (taskDelay) {
                Thread.sleep(taskDelay, { println "thread interupted " })
                log.debug "ActionTask: finished delay $taskDelay ms, now scheduling task"
            }

            step.ctx?.flowListeners.each { listener ->
                listener.beforeFlowNodeExecuteState(step.ctx, this)
            }
            //schedule task and receive the future and store it
            //pass promise from this into new closure in the task

            cloned()
            List cmdArgs = []
            String command = ""
            if (ctx.newInClosure) {
                List<CommandWithArgumentList> cal = ctx.newInClosure.grep { it instanceof CommandWithArgumentList }.asList()

                cal.each {
                    command = it.name               //keeps overwritting the name
                    cmdArgs.addAll(it.toList())
                }
            }

            //args can get padded with null arg at the end of the list.  So if see the null, then strip it off
            if (args != null) {
                def size = args.size()
                if (args[size - 1] == null)
                    args = args.toList().subList(0, size - 1)
            }

            Promise promise = task {
                def initialSize = 4096
                ByteArrayOutputStream err = new ByteArrayOutputStream(initialSize)
                def processError = ""

                ProcessBuilder processBldr = new ProcessBuilder()
                processBldr.command("powershell.exe", "$command", *cmdArgs)
                def process = processBldr.start()
                process.consumeProcessErrorStream(err)
                def ans = process.text
                int exitCode = process.waitFor()
                if (exitCode != 0) {
                    processError = err.toString()
                }

                exitCode == 0 ? ans : processError

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

                if (resultValue instanceof Exception) {
                    if (errHandler) {
                        log.debug "PowerShell actionTask(): task hit exception $resultValue"
                        status = FlowNodeStatus.errors
                        this.errors << resultValue
                        errHandler(resultValue, this)
                    }

                }

                log.debug "PowerShell actionTask(): promise was bound, removed the promise from activePromises: $yesNo, and activePromises : " + ctx?.activePromises

            }

            step
        } catch (Exception e) {
            if (errHandler) {
                log.debug "PowerShell actionTask(): hit exception $e"
                status = FlowNodeStatus.errors
                this.errors << e
                errHandler(e, this)
            }
            step
        }

    }

    def then(AbstractFlowNode nextStep, args = null, Closure errHandler = null) {
        assert nextStep, "was expecting non null nextStep"

        //todo replace with log
        log.debug "then : fire <$nextStep.name> action "

        Object[] EMPTY_ARGS = []

        super.then(nextStep, errHandler)

        def previousTask = this
        pshActionTask(previousTask, nextStep, args ?: EMPTY_ARGS)

    }

    String toString() {
        if (debug == false)
            "PowerShellAction (name:$name, status:$status)"
        else
            "PowerShellAction (name:$name, status:$status, action:${action.toString()})"
    }
}

