package com.softwood.rules.core

import com.softwood.rules.api.Action
//todo look at java lambda interface pasisng using function signature rather than closure
// import java.util.function.

/**
 * base Action implementation
 * invokes the closure and returns the result
 * if param is passed to action the delagate for doAction is set to parameter passed
 */
class BasicAction implements Action {

    String name = ""
    String description = ""
    private Closure doAction = {}  //do nothing

    void setAction (Closure c) {
        doAction = c.clone()
        doAction.resolveStrategy = Closure.DELEGATE_FIRST
    }

    Closure getAction () {
        doAction
    }

    def execute (param = null ) {
        assert doAction
        if (param != null) {
            doAction.delegate = param

            doAction()

        } else
            doAction()
    }

    def call (param = null) {
        execute (param)
    }
}