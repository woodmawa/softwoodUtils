package com.softwood.flow.core.flows

import com.softwood.flow.core.languageElements.Condition

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

class FlowContext extends Expando {
    ConcurrentLinkedDeque activePromises
    ConcurrentLinkedDeque promises
    ConcurrentLinkedQueue taskActions
    ConcurrentLinkedQueue flowListeners
    ConcurrentLinkedQueue newInClosure
    ConcurrentHashMap newInClosureMap
    ConcurrentHashMap flowNodeResults

    Flow flow
    FlowType type
    Object[] initialArgs

    static FlowContext newProcessContext (flow) {
        FlowContext ctx = new FlowContext()
        ctx.flow = flow
        ctx.type = FlowType.Process
        ctx
    }

    static Expando newFreeStandingContext () {
        FlowContext ctx = new FlowContext()
        ctx.activePromises = new ConcurrentLinkedDeque<>()
        ctx.promises = new ConcurrentLinkedDeque<>()
        ctx.taskActions = new ConcurrentLinkedQueue<>()
        ctx.newInClosureMap = new ConcurrentHashMap()
        ctx.flowNodeResults = new ConcurrentHashMap()
        ctx.newInClosure = new ConcurrentLinkedQueue<>()
        ctx.flow = null
        ctx.type = FlowType.FreeStanding
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
    }

    //Made this a closure so that the whens delegate can be set, and we can resolve 'newInClosure'
    //however there can only be one closure with this name - but we would like to take either a condition or
    //a boolean.  SO we need to switch on the type of someCondition
    def when = {  someCondition, toDoArgs, Closure toDo ->

        toDo.delegate = delegate
        toDo.resolveStrategy = Closure.DELEGATE_FIRST

        boolean outcome = false
        switch (someCondition?.class) {
            case  Condition :
                if (someCondition )
                    outcome = someCondition.test ()  //run the test
                else
                    outcome = false
                break
            case Closure :
                outcome = someCondition.call()
                break
            case Boolean :
                outcome = someCondition
                break
            default:
                outcome = false
                break
        }

        if (outcome) {
            if (toDoArgs && toDoArgs instanceof Object[] )
                toDo (*toDoArgs)
            else
                toDo (toDoArgs)
        } else
            outcome       //fail as default

    }

    def flowCondition = {argToTest, Closure condClosure ->

        Condition.newCondition(argToTest, condClosure)
    }


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
