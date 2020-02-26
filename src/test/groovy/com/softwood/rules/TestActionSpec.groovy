package com.softwood.rules

import com.softwood.rules.api.Action
import com.softwood.rules.api.Condition
import com.softwood.rules.core.BasicAction
import com.softwood.rules.core.BasicCondition
import spock.lang.Specification

class TestActionSpec extends Specification {

    def "test BasicAction with new closure set to doAction" () {
        given:
        String param = "william"
        def output
        Closure doIt = {output = "did your action"}
        Action action = new BasicAction (name:"act#1", description:"do something", action: doIt)

        when:
        def  result = action.execute(param)

        then:
        action.name == "act#1"
        action.description == "do something"
        action.action.resolveStrategy == Closure.DELEGATE_FIRST
        action.action.delegate == param
        output == "did your action"
        action.priority == 0

    }
}
