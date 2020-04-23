package com.softwood.flow.core.flows

import com.softwood.flow.core.languageElements.Condition
import com.softwood.flow.core.nodes.AbstractFlowNode
import com.softwood.flow.core.nodes.ChoiceAction
import com.softwood.flow.core.nodes.TaskAction
import groovyx.gpars.dataflow.DataflowVariable
import org.codehaus.groovy.runtime.MethodClosure

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

class FlowContext extends Expando {

   static FlowContext newProcessContext (flow) {
        FlowContext ctx = new FlowContext()
        ctx.flow = flow
        ctx.type = FlowType.Process
        ctx
    }

    static FlowContext newFreeStandingContext () {
        FlowContext ctx = new FlowContext()
        ctx.activePromises = new ConcurrentLinkedDeque<>()
        ctx.promises = new ConcurrentLinkedDeque<>()
        ctx.taskActions = new ConcurrentLinkedQueue<>()
        ctx.newInClosureMap = new ConcurrentHashMap()
        ctx.flowNodeResults = new ConcurrentHashMap()
        ctx.newInClosure = new ConcurrentLinkedQueue<>()
        ctx.flow = null
        ctx.type = FlowType.FreeStanding
        ctx.initialArgs = []
        ctx
     }

    FlowContext() {
        activePromises = new ConcurrentLinkedDeque<>()
        promises = new ConcurrentLinkedDeque<>()
        taskActions = new ConcurrentLinkedQueue<>()
        flowListeners = new ConcurrentLinkedQueue<FlowListener>()
        newInClosureMap = new ConcurrentHashMap()
        flowNodeResults = new ConcurrentHashMap()
        newInClosure = new ConcurrentLinkedQueue<>()
        flow = null
        type = FlowType.Process
        initialArgs = []
    }

    /* ensure that if we have this property we we use it, else just use the Expandos internal map */
    def getProperty (String name) {

        List<PropertyValue> metaProps = getMetaClass().getMetaPropertyValues()
        MetaProperty prop = getMetaClass().getMetaProperty(name)
        if (prop) {
            prop.getProperty(this)
        } else {
            //if not real property - get the expando property map and try and look in there using 'name' arg as the key
            def result

            Map m = getProperties ()
            m.get(name)
        }
    }

    /* ensure that if we have this property we we use it, else just use the Expandos internal map */
    void setProperty (String name, value ) {

        List<PropertyValue> metaProps = getMetaClass().getMetaPropertyValues()
        MetaProperty prop = getMetaClass().getMetaProperty(name)
        if (prop) {
            prop.setProperty(this, value)
        } else {
            //if not real property - get the expando property map and try and look in there using 'name' arg as the key
            def result

            Map m = getProperties ()
            m.put(name, value)
        }
    }


    /**
     * thre types of when, one with condition (test it), one with closure (eval it) or with boolean
     *
     * if boolean truth for the when is tru then run the attached closure, with arg or spread args as appropriate
     *
     * @param someCondition should eval to true/false when tested
     * @param toDoArgs - args for the closure
     * @param workToDoClosure - if true trigger logic to run
     * @return def
     **/

    def when ( Condition SomeCondition, toDoArgs, @DelegatesTo(FlowContext) Closure toDo) {

        toDo.delegate = this
        toDo.resolveStrategy = Closure.DELEGATE_FIRST

        boolean outcome = false
        if (SomeCondition )
            outcome = SomeCondition.test ()  //run the test
        else
            outcome = false

        if (outcome) {
            if (toDoArgs && toDoArgs instanceof Object[] )
                toDo (*toDoArgs)
            else
                toDo (toDoArgs)
        } else
            outcome       //fail as default

    }

    def when (boolean someBoolean, toDoArgs, @DelegatesTo(FlowContext) Closure toDo) {
        toDo.delegate = this
        toDo.resolveStrategy = Closure.DELEGATE_FIRST

        if (someBoolean) {
            if (toDoArgs && toDoArgs instanceof Object[] )
                toDo (*toDoArgs)
            else
                toDo (toDoArgs)
        } else
            false       //fail as default

    }

    def when (Closure someClosure, toDoArgs, @DelegatesTo(FlowContext) Closure toDo) {
        toDo.delegate = this
        toDo.resolveStrategy = Closure.DELEGATE_FIRST

        boolean  outcome = false
        if (someClosure)
            outcome = someClosure()

        if (outcome) {
            if (toDoArgs && toDoArgs instanceof Object[] )
                toDo (*toDoArgs)
            else
                toDo (toDoArgs)
        } else
            false       //fail as default

    }

    //declares flowCondition as usable variable into the context
    MethodClosure flowCondition = Condition::flowConditionClause
    MethodClosure action = TaskAction::newAction
    MethodClosure choice = ChoiceAction::newChoiceAction
    MethodClosure subflow = Subflow::newSubflow

    void saveClosureNewIns (clos, newInItem) {
        def key = getLogicalAddress (clos)
        ConcurrentLinkedQueue newIns = newInClosureMap.computeIfAbsent (key, {k -> new ConcurrentLinkedQueue()})
        newIns << newInItem
        newInClosureMap.put (key, newIns)
    }

    ConcurrentLinkedQueue retrieveClosureNewIns (clos) {
        def key = getLogicalAddress (clos)

        newInClosureMap.computeIfAbsent (key, {k -> new ConcurrentLinkedQueue()})
    }

    def completedClosureNewIns (clos) {
        newInClosureMap.remove(getLogicalAddress (clos))
    }

    String  getLogicalAddress (ofItem) {
        String address = Integer.toHexString (ofItem.hashCode())
        "${ofItem.getClass()}@$address"
    }

    boolean isRunningInClosure (someObject) {
        someObject.hasProperty ('owner')
    }

}
