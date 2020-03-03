package com.softwood.rules.core

import com.softwood.rules.api.Action

import java.util.concurrent.ConcurrentHashMap

//todo look at java lambda interface pasisng using function signature rather than closure
// import java.util.function.

/**
 * base Action implementation
 * invokes the closure and returns the result
 * if param is passed to action the delegate for doAction is set to parameter passed
 *
 * could have used a closure as delegate i think ...
 */
class BasicAction implements Action {

    String name = ""
    String description = ""
    private Closure doAction = {arg -> "No Action"}  //do nothing

    //enable action to carry sata data
    Map stateData = new ConcurrentHashMap<>()

    //dont permit chnage of state from without the action
    Map getStateData () {
        stateData.asImmutable()
    }

    void setStateData (Map state) {
        assert state
        stateData.putAll(new ConcurrentHashMap<>(state))
    }

    void clearStateData() {
        stateData.clear()
    }

    void setAction (Closure c) {
        doAction = c.clone()
        doAction.resolveStrategy = Closure.DELEGATE_FIRST
    }

    Closure getAction () {
        doAction
    }

    def execute (param = null ) {
        assert doAction

        def result
        if (param != null) {
            //set the action as the delegate for doAction.  closure can call getStateData() for action state
            doAction.delegate = this

            result = doAction(param)

        } else
            result = doAction()

        result
    }

    def call (param = null) {
        execute (param)
    }
}