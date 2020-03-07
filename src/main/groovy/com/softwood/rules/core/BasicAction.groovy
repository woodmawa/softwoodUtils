package com.softwood.rules.core

import com.softwood.rules.api.Action
import groovy.transform.MapConstructor
import groovy.transform.ToString
import groovy.util.logging.Slf4j

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
@MapConstructor
@Slf4j
class BasicAction implements Action {

    String name = ""
    String description = ""
    Closure doAction = {arg -> "Default: No Action"}  //do nothing

    //enable action to carry state data if required as context for doAction closure
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
        //set the this action as the delegate for doAction.  closure can call getStateData() for action state
        doAction.delegate = this
    }

    void setDoAction (Closure c) {
        setAction c
    }

    Closure getAction () {
        doAction
    }

    @Override
    def invoke(param = null ) {
        assert doAction

        def result
        if (param != null) {

            if (param)
                log.debug "Action $this, running doAction closure with $param"
            else
                log.debug "Action $this, running doAction closure, with no params"

            if (doAction.maximumNumberOfParameters >= 1)
                result = doAction(param)
            else
                result = doAction()

        } else {
            log.debug "Action $this, running doAction closure, with no params"
            result = doAction()
        }

        result
    }

    def call (param = null) {
        invoke (param)
    }

    String toString () {
        "$this.class.name ($name) "
    }
}