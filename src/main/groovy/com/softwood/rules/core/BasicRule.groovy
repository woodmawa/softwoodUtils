package com.softwood.rules.core

import com.softwood.rules.api.Action
import com.softwood.rules.api.Condition
import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

/**
 * basic rule has a collection of preconditions that must all be true
 * before the action will be invoked
 *
 */

@EqualsAndHashCode(includes = ["name", "description", "priority"])
class BasicRule implements Rule, Comparable {

    /**
     * Rule name.
     */
    String name = "unnamed"

    /**
     * Rule description.
     */
    String description = "unspecified"

    /**
     * Rule priority.
     */
    int priority = 0

    Collection<Predicate> preConditions = new ConcurrentLinkedDeque<>()

    //set action to an action that does nothing - returns "Do nothing action" by default
    Action action  = new BasicAction (name: "basicAction", description: "Do nothing action")

    void setAction (Closure action) {
        assert action
        action = new BasicAction (name:"anonymousAction", description:"anonymous action", action )
    }

    Action shiftleft (Closure action) {
        assert action
        action = new BasicAction (name:"anonymousAction", description:"anonymous action", action )
        this
    }

    /**
     * checks each fact angainst any preConditions
     * @param facts
     * @return
     */
    private boolean checkPreconditions (Facts facts) {

        if (preConditions.size() == 0) {   //no pre conditions so just return true
            println "this rule $this has no preconditions, return true"
            return true
        }

        AtomicBoolean preConditionsCheck = new AtomicBoolean (true)

        //serial at the mo parallelise later
        List<Fact> list = facts.asList()
        list.each {Fact fact ->     //essentially this is a Map.Entry
            preConditions.each {Predicate condition ->
                def testRes = condition.test(fact)
                preConditionsCheck.getAndSet(preConditionsCheck.get() && condition.test(fact))

            }
        }
        preConditionsCheck.get()
    }


    boolean evaluate(Facts facts, param = null) {
        return checkPreconditions (facts)
    }

    def execute (Facts facts, param = null) {

        if (checkPreconditions(facts)) {
            def ret = (param ? action.execute(param) : action.execute())
            ret
        } else {
            println "rule evaulate  : pre conditions $preConditions were not met "
            "preconditions not met"
        }
    }


    //compare based on hashCode  of rule
    int compareTo(Rule o) {
        this.hashCode() <=> o.hashCode()

    }

    String toString() {
        "BasicRule(name:$name, description:$description, priority:$priority})"
    }
}
