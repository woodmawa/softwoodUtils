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

    @Bindable HashMap attributes = new ConcurrentHashMap<>()

    public void addAttribute (String key, value) {
        assert key

        def oldValue = attributes.clone()
        attributes.put (key, value )
        firePropertyChange('attributes', oldValue, attributes)
    }

    public void removeAttribute (String key) {
        assert key

        def oldValue = attributes.clone()
        attributes.remove (key, value )
        firePropertyChange('attributes', oldValue, attributes)
    }


    void leftShift (Map attribs) {
        assert attribs

        def oldValue = attributes.clone()
        attributes << attribs
        firePropertyChange('attributes', oldValue, attributes)
    }


    public List<RuleListener> getRuleListeners() {
        return ruleListeners.toList()
    }

    public List<RuleEngineListener> getRulesEngineListeners() {
        return ruleEngineListeners.toList()
    }

    public void registerRulesEngineListener(RuleEngineListener ruleEngineListener) {
        rulesEngineListeners.add(ruleEngineListener)
    }

    public void registerRuleListener(RuleListener ruleListener) {
        ruleListeners.add(ruleListener)
    }

    public void removeRuleListener (RuleListener listener) {
        ruleListeners.remove(listener)
    }


    public void registerRuleListeners(List<RuleListener> ruleListeners) {
        this.ruleListeners.addAll(ruleListeners)
    }

    public void registerRulesEngineListeners(List<RuleEngineListener> ruleEngineListeners) {
        this.rulesEngineListeners.addAll(ruleEngineListeners);
    }

    public void removeRuleEngineListener (RuleEngineListener listener) {
        ruleEngineListeners.remove(listener)
    }
}
