package com.softwood.rules

import com.softwood.rules.api.Condition
import com.softwood.rules.core.BasicCondition
import spock.lang.Specification

class TestConditionSpec extends Specification {

    def "test BasicCondition with updated dynamicTest" () {
        given:
        String param = "william"
        Condition condition = new BasicCondition(name: "c1#", description : "my first condition", dynamicTest: {it == "william"})

        when:
        def  result = condition.test(param)

        then:
        condition.name == "c1#"
        condition.description == "my first condition"
        result == true
    }
}
