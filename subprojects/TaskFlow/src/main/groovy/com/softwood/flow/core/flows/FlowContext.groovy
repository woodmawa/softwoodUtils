package com.softwood.flow.core.flows


import com.softwood.flow.core.languageElements.Condition
import com.softwood.flow.core.languageElements.CommandWithArgumentList
import com.softwood.flow.core.nodes.AbstractFlowNode
import com.softwood.flow.core.nodes.ChoiceAction
import com.softwood.flow.core.nodes.CmdShellAction
import com.softwood.flow.core.nodes.PowerShellAction
import com.softwood.flow.core.nodes.TaskAction
import org.codehaus.groovy.runtime.MethodClosure

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

class FlowContext extends Expando {

    ConcurrentLinkedQueue taskActions
    ConcurrentLinkedQueue flowListeners
    ConcurrentLinkedQueue initialArgs
    ConcurrentLinkedQueue errors
    ConcurrentLinkedDeque  newInClosureStack
    ConcurrentLinkedQueue newInClosure
    AbstractFlow flow
    FlowType type


   static FlowContext newProcessContext (flow) {
        FlowContext ctx = new FlowContext()
        ctx.errors = new ConcurrentLinkedQueue<>()
        ctx.flow = flow
        ctx.type = FlowType.Process
        ctx
    }

    static FlowContext newFreeStandingContext () {
        FlowContext ctx = new FlowContext()
        ctx.activePromises = new ConcurrentLinkedDeque<>()
        ctx.promises = new ConcurrentLinkedDeque<>()
        ctx.taskActions = new ConcurrentLinkedQueue<>()
        ctx.errors = new ConcurrentLinkedQueue<>()
        ctx.newInClosureStack = new ConcurrentLinkedDeque()
        ctx.flowNodeResults = new ConcurrentHashMap()
        ctx.newInClosure = new ConcurrentLinkedQueue<>()
        ctx.flow = null
        ctx.type = FlowType.FreeStanding
        ctx.initialArgs = new ConcurrentLinkedQueue<>()
        ctx
     }

    FlowContext() {
        activePromises = new ConcurrentLinkedDeque<>()
        promises = new ConcurrentLinkedDeque<>()
        taskActions = new ConcurrentLinkedQueue<>()
        errors = new ConcurrentLinkedQueue<>()
        flowListeners = new ConcurrentLinkedQueue<FlowListener>()
        newInClosureStack  = new ConcurrentLinkedDeque<>()

        flowNodeResults = new ConcurrentHashMap()
        newInClosure = new ConcurrentLinkedQueue<>()
        flow = null
        type = FlowType.Process
        initialArgs = new ConcurrentLinkedQueue<>()
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
     * utility method to create save existing context and start fresh newInsClosure
     * @param method
     * @return
     */
    def withNestedNewIns (Closure method, AbstractFlowNode previous, AbstractFlowNode step,  Object [] args, Closure errHandler = null) {
        assert method
        newInClosureStack.push (newInClosure)       //save current context and start new one
        newInClosure = new ConcurrentLinkedQueue()  //create a new empty list

        def result
        if (method.parameterTypes?[-1] instanceof Closure) {
            if (args?.size() > 0)
                result = method(previous, step, *args, errHandler)
            else if (args != null)
                result = method(previous, step, args, errHandler)
            else
                result = method(previous, step, errHandler)
        } else {
            if (args?.size() > 0)
                result = method(previous, step, *args)
            else if (args != null)
                result = method(previous, step, args)
            else
                result = method(previous, step, null)
        }

        //if necessary could process newIns here but its assumed to have been done by the method()
        newInClosure.clear()
        newInClosure = newInClosureStack.pop ()
        result
    }

    def withNestedNewIns (Closure method, ArrayList arrayArgs = null,   Object [] args, Closure errHandler = null) {
        assert method
        newInClosureStack.push (newInClosure)       //save current context and start new one
        newInClosure = new ConcurrentLinkedQueue()  //create a new empty list

        def result
        if (method.parameterTypes?[-1] instanceof Closure) {
            if (args?.size() > 0)
                result = method(*args, errHandler)
            else if (args != null)
                result = method(args, errHandler)
            else
                result = method(errHandler)
        } else {
            if (args?.size() > 0)
                result = method(*args)
            else if (args != null)
                result = method(args)
            else
                result = method()

        }

        //if necessary could process newIns here but its assumed to have been done by the method()
        newInClosure.clear()
        newInClosure = newInClosureStack.pop ()
        result
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

    def when ( Condition someCondition, toDoArgs = null, @DelegatesTo(FlowContext) Closure toDo) {

        toDo.delegate = this  //assume when is called in choice closure with ctx as delegate, hence this(ctx) for the to do.  is this correct ??
        toDo.resolveStrategy = Closure.DELEGATE_FIRST

        boolean outcome = false
        if (someCondition )
            outcome = someCondition.test ()  //run the test
        else
            outcome = false

        if (outcome) {
            if (toDoArgs && toDoArgs instanceof Object[] )
                toDo (*toDoArgs)
            else if (toDoArgs)
                toDo (toDoArgs)
            else if (toDoArgs == null)
                toDo (someCondition.defaultItemToTest)  //use the condition default item as arg for the closure
        } else
            outcome       //fail as default

    }

    def when (conditionArgs,  Condition someCondition, toDoArgs = null, @DelegatesTo(FlowContext) Closure toDo) {

        toDo.delegate = this  //assume when is called in choice closure with ctx as delegate, hence this(ctx) for the to do.  is this correct ??
        toDo.resolveStrategy = Closure.DELEGATE_FIRST

        boolean outcome = false
        if (someCondition )
            outcome = someCondition.test (conditionArgs)  //run the test
        else
            outcome = false

        if (outcome) {
            if (toDoArgs && toDoArgs instanceof Object[] )
                toDo (*toDoArgs)
            else if (toDoArgs)
                toDo (toDoArgs)
            else if (toDoArgs == null) {
                if (toDo.maximumNumberOfParameters == 1)
                    toDo(someCondition.defaultItemToTest)  //use the condition default item as arg for the closure
                else
                    toDo()
            }
        } else
            outcome       //fail as default

    }

    def when (boolean someBoolean, toDoArgs = null, @DelegatesTo(FlowContext) Closure toDo) {
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

    def when (Closure<Boolean> someClosure, toDoArgs = null, @DelegatesTo(FlowContext) Closure toDo) {
        toDo.delegate = this
        toDo.resolveStrategy = Closure.DELEGATE_FIRST

        boolean  outcome = false
        if (someClosure)
            outcome = someClosure()

        if (outcome) {
            if (toDoArgs && toDoArgs instanceof Object[] )
                toDo (*toDoArgs)
            else if (toDoArgs)
                toDo (toDoArgs)
            else if (toDoArgs == null)
                toDo ()  //use the condition default item as arg for the closure
        } else
            false       //fail as default

    }

    def when (closureArgs, Closure<Boolean> someClosure, toDoArgs = null, @DelegatesTo(FlowContext) Closure toDo) {
        toDo.delegate = this
        toDo.resolveStrategy = Closure.DELEGATE_FIRST

        boolean  outcome = false
        if (someClosure)
            outcome = someClosure(closureArgs)

        if (outcome) {
            if (toDoArgs && toDoArgs instanceof Object[] )
                toDo (*toDoArgs)
            else if (toDoArgs)
                toDo (toDoArgs)
            else if (toDoArgs == null)
                if (toDo.maximumNumberOfParameters == 1)
                    toDo(closureArgs)  //use the condition default item as arg for the closure
                else
                    toDo()
        } else
            false       //fail as default

    }

/*    Closure action = {name = null, Closure closure ->
        TaskAction.newAction(this, name, closure)
    }

    Closure flowCondition = {def condArg, Closure conditionClosure ->
        Condition.newCondition(condArg, conditionClosure )
    }*/
    /**
     * these are DSL methods that can be called from within a flow closure,
     * they add this FlowContext when calling the factory classes for each task type
     *
     * @param Object[] args - constructor args for the factory classes
     * @return instance : factory class instance created
     */

    def subflow (Object[] args) {
        assert args
        Subflow::newSubflow (this, *args)
    }

    def action (Object[] args) {
        assert args
        TaskAction::newAction (this, *args)
    }

    def cmdShell (Object[] args) {
        assert args
        CmdShellAction::newCmdShellAction (this, *args)
    }

    def pshShell (Object[] args) {
        assert args
        PowerShellAction::newPowerShellAction (this, *args)
    }

    def flowCondition (Object[] args) {
        assert args
        Condition::newCondition (*args)
    }

    def choice (Object[] args) {
        assert args
        ChoiceAction::newChoiceAction (this, *args)
    }
    //declares flowCondition as usable variable into the context
    //MethodClosure flowCondition = Condition::newCondition
    //MethodClosure action = TaskAction::newAction
    //MethodClosure cmdShell = CmdShellAction::cmdActionTask
    //MethodClosure pshShell = PowerShellAction::pshActionTask
    //MethodClosure choice = ChoiceAction::newChoiceAction
    //MethodClosure subflow = Subflow::newSubflow

    Closure cmdWithArguments = {String name, args = null ->
        CommandWithArgumentList::newShellCommand (delegate, name, args)
    }

     Closure pshWithArguments = {String name, args = null ->
        CommandWithArgumentList::newShellCommand (delegate, name, args)
    }

    //MethodClosure cmdArgument = CommandWithArgumentList::newCommandArgumentList
    //MethodClosure pshArgument = CommandWithArgumentList::newCommandArgumentList

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
