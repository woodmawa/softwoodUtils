package com.softwood.rules.core

import com.softwood.rules.api.Action
import com.softwood.rules.api.Condition

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * basic rule has a collection of preconditions that must all be true
 * before the action will be invoked
 *
 */
class BasicRule {

    /**
     * Rule name.
     */
    protected String name

    /**
     * Rule description.
     */
    protected String description

    /**
     * Rule priority.
     */
    protected int priority

    Collection<Condition> preConditions = new ConcurrentLinkedDeque<Condition>()

    Action action  = new BasicAction (name: "basicAction", description: "do nothing basic action")

    def evaluate (Collection facts, param = null) {
        AtomicBoolean checkPreConditions = new AtomicBoolean (false)

        //serial at the mo parallelise later
        facts.each {fact ->
            preConditions.each {Condition condition ->
                checkPreConditions.getAndSet(checkPreConditions.get() && condition.test(fact))

            }
        }
        if (checkPreConditions.get()) {
            def ret = (param ? action.execute(param) : action.execute())
            ret
        }
    }

}
