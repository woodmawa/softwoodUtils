package com.softwood.flow.core.nodes

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.FlowEvent
import com.softwood.flow.core.flows.FlowType
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.support.CallingStackContext
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise

import java.util.concurrent.ConcurrentLinkedQueue

@Slf4j
class ChoiceAction extends AbstractFlowNode {

    ConcurrentLinkedQueue<Subflow> choiceSubflows = new ConcurrentLinkedQueue<>()
    def selectValue

    def setSelectValue(def value) {
        selectValue = value
    }

    static ChoiceAction newChoiceAction(FlowContext ctx, String name = null, Closure closure) {
        /*
         * injected ctx to use
         */

        def choice = new ChoiceAction(ctx: ctx, name: name ?: "anonymous", action: closure)
        choice.ctx?.taskActions << choice
        choice.ctx?.newInClosure << choice  //add to items generated within the running closure

        choice

    }

    /**
     * run the closure as task, by invoking private doRun().  result holds the answer as a DF promise
     * @param args - optional Object[]
     * @param errHandler - closure to call in case of exception being triggered
     * @return 'this' FlowNode
     */
    def fork(args = null, Closure errHandler = null) {
        doFork(null, args, errHandler)
    }

    def fork(AbstractFlowNode previousNode, args = null, Closure errHandler = null) {
        doFork(previousNode, args, errHandler)
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
    protected doFork(AbstractFlowNode previousNode, args = null, Closure errHandler = null) {

        def choice
        choice = ctx.withNestedNewIns(this::choiceTask, previousNode, this, args, errHandler)
        //def choice = choiceTask(previousNode, this, args, errHandler)
        choice
    }

    protected def choiceTask(TaskAction previousNode, AbstractFlowNode step, args, Closure errHandler = null) {
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

            //ctx.newInClosureStack.push (ctx.newInClosure)
            //ctx.newInClosure = new ConcurrentLinkedQueue<>()  //setup new frame

            //as this is a choice node - no point running as a task
            //todo  - where should the calculator live ?  here or in the original closure
            def selectorValue = subflowSelector(previousNode, args)

            //cloned delegate is the FlowContext so no point passing as a parameter
            if (args instanceof Object[])
                step.result << cloned(selectorValue, *args)  //(calculated selector discriminator, args...)
            else if (args) {
                if (cloned.maximumNumberOfParameters == 2)
                    step.result << cloned(selectorValue, args)
                else
                    step.result << cloned(selectorValue)
            } else {
                //in case closure takes two args, but we have only one - just pass a null
                if (cloned.maximumNumberOfParameters == 2)
                    step.result << cloned(selectorValue, null)
                else
                    step.result << cloned(selectorValue)
            }

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
            def newSubflows = newIns.grep { it.class == Subflow }
            def newActions = newIns.grep { it instanceof AbstractFlowNode }

            //if you dont decalare a subflow - quietly create one and add the actions into it
            if (!newSubflows && newActions) {
                Subflow defaultSubflow = new Subflow(ctx: ctx, name: "choice[$name].defaultSubflow", subflowClosure: { "choice [$name] subflow done" })
                defaultSubflow.parent = ctx.flow
                defaultSubflow << newActions
                newSubflows = [defaultSubflow]
            }

            choiceSubflows.addAll(newSubflows)

            if (ctx.flow) {
                choiceSubflows.each { it.parent = ctx.flow; ctx.flow.subflows << it }
            }

            //for each subflow declared in the choice closure execute each subflow and its nodes
            choiceSubflows.each { sflow ->
                //for each flow that makes it run each flow, use choice args if set else Queue of run task actions
                if (args)
                    sflow.run(args)
                else
                    sflow.run()  //ctx.taskActions - but this will be all including the actions in this sflow
            }

            step
        } catch (Exception e) {
            if (errHandler) {
                log.debug "choiceTask()  hit exception $e"
                status = FlowNodeStatus.errors
                this.errors << e
                errHandler(e, this)
            }
            step
        }
    }

    //default selector logic for a choiceAction, this can be changed for anly choice by explicitly setting the calculator
    def subflowSelector(AbstractFlowNode previousNode, args) {

        def previousResult
        //ovveride option choice
        if (selectValue != null)
            previousResult = selectValue
        else if (previousNode) {
            previousResult = previousNode.result.val
        } else
            previousResult = args    //default 'null logic' position

        previousResult
    }

    String toString() {
        int sz = choiceSubflows.size()
        String insertTxt = "with # of subflows ${choiceSubflows.size()}"
        "Choice (name:$name, status:$status) ${sz ? insertTxt : ''}"
    }
}
