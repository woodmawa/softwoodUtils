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
    String name

    /**
     * Rule description.
     */
    String description

    /**
     * Rule priority.
     */
    int priority

    Collection<Predicate> preConditions = new ConcurrentLinkedDeque<Predicate>()

    Action action  = new BasicAction (name: "basicAction", description: "do nothing basic action")

    /**
     * checks each fact angainst any preConditions
     * @param facts
     * @return
     */
    private boolean checkPreconditions (facts) {
        AtomicBoolean checkPreConditions = new AtomicBoolean (false)

        //serial at the mo parallelise later
        facts.each {fact ->
            preConditions.each {Predicate condition ->
                checkPreConditions.getAndSet(checkPreConditions.get() && condition.test(fact))

            }
        }
        checkPreConditions.get()
    }

    def evaluate (Collection<Fact> facts, param = null) {
       checkPreconditions(facts)
    }

    def execute (Collection<Fact> facts, param = null) {

        if (checkPreconditions()) {
            def ret = (param ? action.execute(param) : action.execute())
            ret
        } else {
            println "rule evaulate  : pre conditions $preConditions were not met "
        }
    }

    boolean evaluate(Facts facts) {
        return evaluate (facts as Collection)
    }

    def execute (Facts facts) {
        return false
    }

    int compareTo(Rule o) {
        return 0
    }
}
