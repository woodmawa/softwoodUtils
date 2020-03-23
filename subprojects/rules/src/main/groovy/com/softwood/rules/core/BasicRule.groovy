package com.softwood.rules.core


import com.softwood.rules.api.Action
import com.softwood.rules.api.Condition
import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts
import com.softwood.rules.api.Rule
import com.softwood.rules.api.RuleFactory
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j
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
@Slf4j
@CompileStatic
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

    void addAllPreConditions(Collection<Condition> conditions) {
        assert conditions
        preConditions.addAll(conditions)
    }

    void AddPreCondition(Condition condition) {
        assert condition
        preConditions.add condition
    }

    void removePreCondition(Condition condition) {
        if (preConditions.contains(condition))
            preConditions.remove(condition)
    }

    void clearAllPreConditions (){
        preConditions.clear()
    }

    List<MethodClosure> getEffectsList () {
        postActionEffects.toList()
    }

    void setAction (@DelegatesTo (Action) Closure task) {
        assert task
        action = RuleFactory.newAction (name:"anonymousAction", description:"anonymous action", task )
    }

    void setAction (Action action) {
        assert action
        this.action = action
    }


    Rule shiftLeft (Closure task) {
        assert action
        action = RuleFactory.newAction (name:"anonymousAction", description:"anonymous action", task )
        this
    }

    /**
     * checks each fact angainst any preConditions
     * @param facts
     * @return
     */
    private boolean checkPreconditions (Facts facts) {

        if (preConditions.size() == 0) {   //no pre conditions so just return true
            log.debug ("rule <$this> has no preconditions, return true")
            return true
        }

        //start false
        AtomicBoolean preConditionsCheck = new AtomicBoolean (false)

        //serial at the mo parallelise later

        //for each fact in facts
        List<Fact> list = facts.asList()
        list.each {Fact fact ->
            log.debug "test $fact, against rule preConditions"
            preConditions.each {Predicate condition ->
                def testRes = condition.test(fact)
                preConditionsCheck.compareAndSet(false, condition.test(fact) )
                log.debug "\tcondition $condition, performed test on $fact, set  rule preConditions check state to : ${preConditionsCheck.get()}"

            }
        }
        log.debug "com.softwood.rules preConditions final check state is ${preConditionsCheck.get()}"
        preConditionsCheck.get()
    }

    private def applyPostActionEffects (arg = null) {
        //apply each effect if it has any defined passing in the optional arg to which the effect applies
        log.debug "applying any post rule execution effects with arg : <$arg>"
        postActionEffects.each { effect ->
            if (effect.maximumNumberOfParameters > 0 )
               effect(arg)
            else
                effect()
        }
    }

    boolean evaluate(Facts facts, param = null) {
        return checkPreconditions (facts)
    }

    def execute (Facts facts, arg = null) {

        def result
        if (checkPreconditions(facts)) {
            try {
            result = (arg ? action.invoke(arg) : action.invoke())
            }
            catch (Exception e) {
                log.debug "rule execute : threw exception, no post action effects were run   " + e.stackTrace
                return "threw exception"
            }

            applyPostActionEffects(arg)
            result
        } else {
            log.debug  "rule execute   : pre conditions $preConditions were not met "
            return "preconditions not met"
        }
    }

    /*
     * forced execution of the rule action, no preconditions check is applied
     * if any effects are enabled then execute them as well
     */
    def justExecute (arg = null) {
        log.debug  "rule justExecute   : invoked action with $arg "

        def result
        try {
            result = action.invoke(arg)
        } catch (Exception e) {
            log.debug "rule justExecute : threw exception, no post action effects were run   " + e.stackTrace
            return "threw exception"
        }

        applyPostActionEffects(arg)
        result
    }


    //compare based on hashCode  of rule
    int compareTo(Rule o) {
        this.hashCode() <=> o.hashCode()

    }

    String toString() {
        "BasicRule(name:$name, description:$description, priority:$priority})"
    }
}
