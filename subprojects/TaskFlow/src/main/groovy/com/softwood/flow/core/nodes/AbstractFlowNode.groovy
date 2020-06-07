package com.softwood.flow.core.nodes

import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowVariable

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import com.softwood.flow.core.flows.FlowContext

enum FlowNodeStatus {
    ready, running, deferred, completed, errors
}

@Slf4j
abstract class AbstractFlowNode {
    static AtomicLong sequenceGenerator = new AtomicLong(0)

    protected Closure action

    protected FlowContext ctx    //has to be static to get splits 'action's' to have a ctx
    protected AbstractFlowNode previousNode
    protected Closure cloned  //'then' clones the closure on the next Action and invokes that
    protected final sequence = sequenceGenerator.incrementAndGet()
    protected ConcurrentLinkedQueue errors = new ConcurrentLinkedQueue()

    String name = "anonymous"
    FlowNodeStatus status = FlowNodeStatus.ready
    protected DataflowVariable result = new DataflowVariable()
    long taskDelay = 0

    protected ConcurrentLinkedQueue dependencies = new ConcurrentLinkedQueue<>()

    def dependsOn(Iterable taskList) {
        dependencies.addAll(taskList)
    }

    def dependsOn(AbstractFlowNode task) {
        dependencies.add(task)
    }

    def dependsOn(String taskName) {
        //todo should this do a look up instead ?
        dependencies.add(taskName)
    }

    def getDependencies() {
        dependencies
    }


    void setResultValue(value) {
        result << value
    }


    //provide a version that unwraps the DF result to get the value
    def getResultValue() {
        def val = result.getVal()     //blocking get on DF result
        if (status != FlowNodeStatus.completed)
            status = FlowNodeStatus.completed       //close small possible timing gap
        val
    }

    def getResultValue(long timeout, TimeUnit unit) {
        def val = result.getVal(timeout, unit)     //blocking get on DF result
    }

    /**
     * if you need to run the process more than once, you'll need to reset each task to permit the task to be rerun and
     * new result set
     *
     */
    void resetFlowNode() {
        assert result.bound == true

        errors.clear()
        result = new DataflowVariable()
        status = FlowNodeStatus.ready
    }

    def then(AbstractFlowNode nextStep, Closure errhandler = null) {
        assert nextStep
        nextStep.previousNode = this

        cloned = nextStep.action.clone()
        if (nextStep != this)
            nextStep.previousNode = this
        if (taskDelay == 0)
            status = FlowNodeStatus.running
        else
            status = FlowNodeStatus.deferred

        cloned.delegate = ctx

        nextStep.ctx = this.ctx

        //check if any promises have completed and if so remove from list

        nextStep

    }

    //syntactic sugar
    def rightShift(AbstractFlowNode nextStep, Closure errhandler = null) {
        then(nextStep, errhandler)
    }


    def waitForDependencies(AbstractFlowNode node) {
        List allFlowTasks = ctx.taskActions.asList()

        List dependencies = node.getDependencies().asList()

        List<AbstractFlowNode> waitForDependentTasks
        if (dependencies.size() > 0 && dependencies[0] instanceof String) {
            waitForDependentTasks = allFlowTasks.grep { task -> dependencies.contains(task.name) }
        } else {
            waitForDependentTasks = allFlowTasks.grep { task -> task in dependencies }
        }
        def List<DataflowVariable> dependentTaskResultsList = waitForDependentTasks.collect { it.result }
        //todo - timeout or interrupt ? how do we do this

        log.debug "waiting for dependant tasks $dependencies to finish"
        dependentTaskResultsList*.join()
    }

}


