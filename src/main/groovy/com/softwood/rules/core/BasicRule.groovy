package com.softwood.rules.core

import com.softwood.rules.api.Action
import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule
import groovy.transform.EqualsAndHashCode
import groovy.transform.MapConstructor
import org.codehaus.groovy.runtime.MethodClosure

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

/**
 * basic rule has a collection of preconditions that must all be true
 * before the action will be invoked
 *
 */

@MapConstructor
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

    Collection <MethodClosure> postActionEffects = new ConcurrentLinkedQueue<>()

    void setAction (Closure action) {
        assert action
        action = new BasicAction (name:"anonymousAction", description:"anonymous action", action )
    }

    void setAction (Action action) {
        assert action
        this.action = action
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

        //start false
        AtomicBoolean preConditionsCheck = new AtomicBoolean (false)

        //serial at the mo parallelise later

        //for each fact in facts
        List<Fact> list = facts.asList()
        list.each {Fact fact ->
            println "test fact $fact, against rule preConditions"
            preConditions.each {Predicate condition ->
                def testRes = condition.test(fact)
                preConditionsCheck.compareAndSet(false, condition.test(fact) )
                println "condtion.test on $fact, set check state to ${preConditionsCheck.get()}"

            }
        }
        preConditionsCheck.get()
    }

    def applyPostActionEffects (arg = null) {
        //apply each effect if it has any defined passing in the optional arg to which the effect applies
        postActionEffects.each {
            effect -> effect(arg)
        }
    }

    boolean evaluate(Facts facts, param = null) {
        return checkPreconditions (facts)
    }

    def execute (Facts facts, arg = null) {

        if (checkPreconditions(facts)) {
            def ret = (arg ? action.invoke(arg) : action.invoke())
            applyPostActionEffects(arg)
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
