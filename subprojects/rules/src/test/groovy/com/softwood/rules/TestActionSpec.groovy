package com.softwood.rules

import com.softwood.rules.api.Action
import com.softwood.rules.api.RuleFactory
import com.softwood.rules.core.BasicAction
import spock.lang.Specification

class TestActionSpec extends Specification {

    def "test BasicAction with new closure set to doAction"() {
        given:
        String param = "william"
        def output
        Closure doIt = { output = "did your action" }
        Action action = new BasicAction(name: "act#1", description: "do something", action: doIt)
        action.action = doIt

        when:
        def result = action.invoke(param)

        then:
        action.name == "act#1"
        action.description == "do something"
        action.action.resolveStrategy == Closure.DELEGATE_FIRST
        action.action.delegate == action
        output == "did your action"

    }

    def "test action where stateData has been set on the action "() {
        given: "a new action, and set some stateData on the action  "

        def result
        Action action = RuleFactory.newAction(name: "act#1", description: "an action ") { def val = stateData.thing;
            result = val; }
        action.setStateData([thing: "william"])

        when: "we invoke the action "
        action.invoke()

        then: "we can see if closure sets the result "
        result == "william"
    }


}
