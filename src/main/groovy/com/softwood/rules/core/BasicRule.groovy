package com.softwood.rules.core

import com.softwood.rules.api.Action
import com.softwood.rules.api.Condition
import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

/**
 * basic rule has a collection of preconditions that must all be true
 * before the action will be invoked
 *
 */
class BasicRule implements Rule {

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

    Action action  = new BasicAction (name: "basicAction", description: "do nothing basic action")

    /**
     * checks each fact angainst any preConditions
     * @param facts
     * @return
     */
    private boolean checkPreconditions (Facts facts) {
        AtomicBoolean checkPreConditions = new AtomicBoolean (false)

        //serial at the mo parallelise later
        Iterator iter = facts.iterator()
        iter.forEachRemaining {Map.Entry fact ->     //essentially this is a Map.Entry
            def val = fact.value
            def key = fact.key
            int numOfRuleConditions = preConditions.size()
            Condition firstCond = preConditions?[0]
            preConditions.each {Predicate condition ->
                def testRes = condition.test(val)
                checkPreConditions.getAndSet(checkPreConditions.get() && condition.test(fact))

            }
        }
        checkPreConditions.get()
    }


    boolean evaluate(Facts facts, param = null) {
        return checkPreconditions (facts)
    }

    def execute (Collection<Fact> facts, param = null) {

        if (checkPreconditions()) {
            def ret = (param ? action.execute(param) : action.execute())
            ret
        } else {
            println "rule evaulate  : pre conditions $preConditions were not met "
        }
    }

    def execute (Facts facts) {
        return execute (facts as Collection)
    }

    int compareTo(Rule o) {
        return 0
    }
}
