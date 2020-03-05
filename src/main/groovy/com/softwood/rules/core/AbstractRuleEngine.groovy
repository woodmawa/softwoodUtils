package com.softwood.rules.core

import com.softwood.rules.api.RuleListener
import groovy.beans.Bindable
import com.softwood.rules.api.RuleEngineListener

import java.util.concurrent.ConcurrentHashMap

/**
 * enable class to add rule listeners
 *
 *
 */

class AbstractRuleEngine {
    @Bindable List<RuleEngineListener> ruleEngineListeners = []
    @Bindable List<RuleListener> ruleListeners = []

    HashMap attributes = new ConcurrentHashMap<>()
}
