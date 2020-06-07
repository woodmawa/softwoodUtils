package com.softwood.rules.core

import com.softwood.rules.api.RuleListener
import groovy.beans.Bindable
import com.softwood.rules.api.RuleEngineListener

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * enable class to add rule listeners
 *
 *
 */

class AbstractRuleEngine {
    Queue<RuleEngineListener> ruleEngineListeners = new ConcurrentLinkedQueue<>()
    Queue<RuleListener> ruleListeners = new ConcurrentLinkedQueue<>()

    @Bindable
    HashMap attributes = new ConcurrentHashMap<>()

    public void addAttribute(String key, value) {
        assert key

        def oldValue = attributes.clone()
        attributes.put(key, value)
        firePropertyChange('attributes', oldValue, attributes)
    }

    public void removeAttribute(String key) {
        assert key

        def oldValue = attributes.clone()
        attributes.remove(key)
        firePropertyChange('attributes', oldValue, attributes)
    }


    void leftShift(Map attribs) {
        assert attribs

        def oldValue = attributes.clone()
        attributes << attribs
        firePropertyChange('attributes', oldValue, attributes)
    }


    /**
     * process ruleListeners
     *
     */
    public List<RuleListener> getRuleListeners() {
        return ruleListeners.toList()
    }

    public void registerRuleListener(RuleListener ruleListener) {
        ruleListeners.add(ruleListener)
    }

    public void registerRuleListeners(List<RuleListener> ruleListeners) {
        this.ruleListeners.addAll(ruleListeners)
    }

    public void removeRuleListener(RuleListener listener) {
        ruleListeners.remove(listener)
    }


    /**
     * process ruleEngineListeners
     *
     */
    public List<RuleEngineListener> getRuleEngineListeners() {
        return ruleEngineListeners.toList()
    }

    public void registerRuleEngineListener(RuleEngineListener ruleEngineListener) {
        ruleEngineListeners.add(ruleEngineListener)
    }


    public void registerRuleEngineListeners(List<RuleEngineListener> ruleEngineListeners) {
        this.ruleEngineListeners.addAll(ruleEngineListeners);
    }

    public void removeRuleEngineListener(RuleEngineListener listener) {
        ruleEngineListeners.remove(listener)
    }

}
